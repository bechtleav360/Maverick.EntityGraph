package org.av360.maverick.graph.feature.objects.domain;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.objects.model.LocalStorageDetails;
import org.av360.maverick.graph.model.enums.UriSchemes;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.model.vocabulary.SDO;
import org.av360.maverick.graph.services.*;
import org.av360.maverick.graph.store.rdf.fragments.RdfEntity;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Service
@Slf4j(topic = "graph.feat.obj.svc")
public class FileServices {

    public record FileAccessResult(URI file, Flux<DataBuffer> content) {
    }

    private final ContentLocationResolverService filePathResolver;

    private final ValueServices valueServices;

    private final EntityServices entityServices;
    private final SchemaServices schemaServices;

    private final IdentifierServices identifierServices;
    private final DefaultDataBufferFactory dataBufferFactory;

    public FileServices(ContentLocationResolverService filePathResolver, ValueServices valueServices, EntityServices entityServices, SchemaServices schemaServices, IdentifierServices identifierServices) {
        this.filePathResolver = filePathResolver;
        this.valueServices = valueServices;
        this.entityServices = entityServices;
        this.schemaServices = schemaServices;
        this.identifierServices = identifierServices;

        dataBufferFactory = new DefaultDataBufferFactory();
    }

    public Mono<RdfTransaction> store(String entityKey, Flux<DataBuffer> bytes, String prefixedPoperty, String filename, @Nullable String language, Authentication authentication) {
        return entityServices.resolveAndVerify(entityKey, authentication)
                .flatMap(entityId -> Mono.zip(
                        Mono.just(new LocalStorageDetails().setEntityId(entityId).setFilename(filename).setLanguage(language)),
                        identifierServices.asReproducibleIRI(Local.Entities.NS, entityId.getLocalName(), filename),
                        schemaServices.resolvePrefixedName(prefixedPoperty)
                ))
                .flatMap(pair -> Mono.zip(
                        Mono.just(pair.getT1().setFileId(pair.getT2()).setProperty(pair.getT3())),
                        filePathResolver.resolveContentLocation(pair.getT1().getEntityId(), pair.getT1().getIdentifier(), pair.getT1().getFilename(), pair.getT1().getLanguage(), authentication)
                )
                )
                .map(pair -> {
                    LocalStorageDetails sd = pair.getT1();
                    sd.setApiPath(pair.getT2().apiPath());
                    sd.setStoragePath(Path.of(pair.getT2().storageURI()));
                    return sd;
                })
                .doOnNext(contentLocation -> log.debug("Writing file with id '{}' into uri '{}'", contentLocation.getIdentifier().getLocalName(), contentLocation.getStoragePath().toUri()))
                .flatMap(localStorageDetails ->

                        DataBufferUtils.write(bytes, localStorageDetails.getStoragePath(), StandardOpenOption.CREATE)
                                .doOnError(error -> log.error("Failed to store file due to reason: {}", error.getMessage()))
                                .then(Mono.just(localStorageDetails)))
                .map(localStorageDetails -> localStorageDetails.setDetails(localStorageDetails.getStoragePath().toFile()))
                .flatMap(sd -> {
                    ValueFactory vf = SimpleValueFactory.getInstance();

                    ModelBuilder builder = new ModelBuilder()
                            .subject(sd.getIdentifier())
                            .add(RDF.TYPE, SDO.MEDIA_OBJECT)
                            .add(RDF.TYPE, Local.Entities.TYPE_EMBEDDED)
                            .add(SDO.CONTENT_SIZE, vf.createLiteral(sd.getLength()))
                            .add(SDO.UPLOAD_DATE, vf.createLiteral(sd.getLastModified()))
                            .add(SDO.NAME, vf.createLiteral(sd.getFilename()))
                            .add(SDO.CONTENT_LOCATION, vf.createLiteral(sd.getStorageLocation()))
                            .add(SDO.CONTENT_URL, vf.createLiteral(sd.getUriPath()))
                            .add(SDO.SUBJECT_OF, sd.getEntityId());

                    if (StringUtils.hasLength(sd.getLanguage())) {
                        builder.add(SDO.IN_LANGUAGE, vf.createLiteral(sd.getLanguage()));
                    }


                    return valueServices.insertEmbedded(sd.getEntityId(), sd.getProperty(), sd.getIdentifier(), builder.build(), authentication);
                });

    }


    public Mono<FileAccessResult> read(String contentKey, Authentication authentication) {
        return entityServices.resolveAndVerify(contentKey, authentication)
                .flatMap(contentId -> Mono.zip(
                        this.entityServices.get(contentId, authentication),
                        Mono.just(contentId)
                )).flatMap(pair -> {
                    RdfEntity embedded = pair.getT1();
                    IRI contentID = pair.getT2();

                    IRI entityId = embedded.findDistinctValue(contentID, SDO.SUBJECT_OF).filter(Value::isIRI).map(value -> (IRI) value).orElseThrow();
                    String name = embedded.findDistinctValue(contentID, SDO.NAME).filter(Value::isLiteral).map(value -> (Literal) value).map(Value::stringValue).orElseThrow();
                    String lang = embedded.findDistinctValue(contentID, SDO.IN_LANGUAGE).filter(Value::isLiteral).map(value -> (Literal) value).map(Value::stringValue).orElse("");
                    return filePathResolver.resolveContentLocation(entityId, contentID, name, lang, authentication);
                })
                .doOnNext(contentLocation -> log.debug("Loading file with id '{}' stored in uri '{}'", contentKey, contentLocation.storageURI()))
                .flatMap(contentLocation -> {

                    if (!contentLocation.storageURI().getScheme().startsWith(UriSchemes.FILE.toString())) {
                        log.warn("Unsupported scheme resolving a content object in uri: {}", contentLocation);
                        return Mono.empty();
                    }

                    Flux<DataBuffer> content = DataBufferUtils.read(Path.of(contentLocation.storageURI()), dataBufferFactory, 4096, StandardOpenOption.READ).switchIfEmpty(Mono.error(new FileNotFoundException()));
                    return Mono.just(new FileAccessResult(contentLocation.storageURI(), content));
                })
                .doOnError(error -> log.warn("Failed to open file with id '{}' due to error: {}", contentKey, error.getMessage()))
                .doOnSuccess(fileAccessResult -> log.debug("Loading file with id '{}' was completed", contentKey));
    }
}
