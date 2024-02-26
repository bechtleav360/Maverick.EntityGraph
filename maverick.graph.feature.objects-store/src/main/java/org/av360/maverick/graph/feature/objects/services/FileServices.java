package org.av360.maverick.graph.feature.objects.services;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.objects.model.LocalStorageDetails;
import org.av360.maverick.graph.model.annotations.OnRepositoryType;
import org.av360.maverick.graph.model.annotations.RequiresPrivilege;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.model.enums.UriSchemes;
import org.av360.maverick.graph.model.errors.InconsistentModelException;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.model.vocabulary.SDO;
import org.av360.maverick.graph.model.vocabulary.meg.Local;
import org.av360.maverick.graph.services.*;
import org.av360.maverick.graph.services.api.Api;
import org.av360.maverick.graph.store.rdf.fragments.RdfFragment;
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

    public record FileAccessResult(URI file, Flux<DataBuffer> content,  String filename, String language) {
    }

    private final ContentLocationResolverService filePathResolver;

    private final ValueServices valueServices;

    private final EntityServices entityServices;
    private final SchemaServices schemaServices;

    private final IdentifierServices identifierServices;
    private final DefaultDataBufferFactory dataBufferFactory;

    private final Api api;

    public FileServices(ContentLocationResolverService filePathResolver, ValueServices valueServices, EntityServices entityServices, SchemaServices schemaServices, IdentifierServices identifierServices, Api api) {
        this.filePathResolver = filePathResolver;
        this.valueServices = valueServices;
        this.entityServices = entityServices;
        this.schemaServices = schemaServices;
        this.identifierServices = identifierServices;
        this.api = api;

        dataBufferFactory = new DefaultDataBufferFactory();
    }

    @RequiresPrivilege(Authorities.CONTRIBUTOR_VALUE)
    @OnRepositoryType(RepositoryType.ENTITIES)
    public Mono<Transaction> store(String entityKey, Flux<DataBuffer> bytes, String prefixedPoperty, String filename, @Nullable String language, SessionContext ctx) {
        return this.api.entities().select().resolveAndVerify(entityKey, ctx)
                .flatMap(entityId -> Mono.zip(
                        Mono.just(new LocalStorageDetails().setEntityId(entityId).setFilename(filename).setLanguage(language)),
                        identifierServices.asReproducibleLocalIRI(Local.Entities.NS, ctx.getEnvironment(), entityId.getLocalName(), filename),
                        this.api.identifiers().prefixes().resolvePrefixedName(prefixedPoperty)
                ))
                .flatMap(pair -> Mono.zip(
                        Mono.just(pair.getT1().setFileId(pair.getT2()).setProperty(pair.getT3())),
                        filePathResolver.resolveContentLocation(pair.getT1().getEntityId(), pair.getT1().getIdentifier(), pair.getT1().getFilename(), pair.getT1().getLanguage(), ctx)
                )
                )
                .map(pair -> {
                    LocalStorageDetails sd = pair.getT1();
                    sd.setApiPath(pair.getT2().apiPath());
                    sd.setStoragePath(Path.of(pair.getT2().storageURI()));
                    return sd;
                })
                .doOnNext(contentLocation -> log.debug("Writing file with id '{}' into uri '{}'", contentLocation.getIdentifier().getLocalName(), contentLocation.getStoragePath().toUri()))
                .flatMap(localStorageDetails -> DataBufferUtils.write(bytes, localStorageDetails.getStoragePath(), StandardOpenOption.CREATE)
                        .doOnError(error -> log.error("Failed to store file due to reason: {}", error.getMessage()))
                        .then(Mono.just(localStorageDetails))
                )
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


                    return valueServices.insertComposite(sd.getEntityId(), sd.getProperty(), sd.getIdentifier(), builder.build(), ctx.getEnvironment().withRepositoryType(RepositoryType.ENTITIES));
                });

    }

    @RequiresPrivilege(Authorities.READER_VALUE)
    @OnRepositoryType(RepositoryType.ENTITIES)
    public Mono<FileAccessResult> read(String contentKey, SessionContext ctx) {
        return this.api.entities().select().resolveAndVerify(contentKey, ctx)
                .flatMap(contentId -> Mono.zip(
                        this.entityServices.get(contentId, ctx),
                        Mono.just(contentId)
                )).flatMap(pair -> {
                    RdfFragment embedded = pair.getT1();
                    IRI contentID = pair.getT2();

                    try {
                        IRI entityId = embedded.findDistinctValue(contentID, SDO.SUBJECT_OF).filter(Value::isIRI).map(value -> (IRI) value).orElseThrow();
                        String name = embedded.findDistinctValue(contentID, SDO.NAME).filter(Value::isLiteral).map(value -> (Literal) value).map(Value::stringValue).orElseThrow();
                        String lang = embedded.findDistinctValue(contentID, SDO.IN_LANGUAGE).filter(Value::isLiteral).map(value -> (Literal) value).map(Value::stringValue).orElse("");
                        return filePathResolver.resolveContentLocation(entityId, contentID, name, lang, ctx);
                    } catch (InconsistentModelException exception) {
                        return Mono.error(exception);
                    }
                })
                .doOnNext(contentLocation -> log.debug("Loading file with id '{}' stored in uri '{}'", contentKey, contentLocation.storageURI()))
                .flatMap(contentLocation -> {

                    if (!contentLocation.storageURI().getScheme().startsWith(UriSchemes.FILE.toString())) {
                        log.warn("Unsupported scheme resolving a content object in uri: {}", contentLocation);
                        return Mono.empty();
                    }

                    Flux<DataBuffer> content = DataBufferUtils.read(Path.of(contentLocation.storageURI()), dataBufferFactory, 4096, StandardOpenOption.READ).switchIfEmpty(Mono.error(new FileNotFoundException()));
                    return Mono.just(new FileAccessResult(contentLocation.storageURI(), content, contentLocation.filename(), contentLocation.language()));
                })
                .doOnError(error -> log.warn("Failed to open file with id '{}' due to error: {}", contentKey, error.getMessage()))
                .doOnSuccess(fileAccessResult -> log.debug("Loading file with id '{}' was completed", contentKey));
    }
}
