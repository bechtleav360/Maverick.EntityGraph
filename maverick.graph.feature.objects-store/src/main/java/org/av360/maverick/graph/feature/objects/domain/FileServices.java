package org.av360.maverick.graph.feature.objects.domain;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.objects.model.LocalStorageDetails;
import org.av360.maverick.graph.model.identifier.IdentifierFactory;
import org.av360.maverick.graph.model.identifier.LocalIdentifier;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.model.vocabulary.SDO;
import org.av360.maverick.graph.services.EntityServices;
import org.av360.maverick.graph.services.SchemaServices;
import org.av360.maverick.graph.services.ValueServices;
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
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Service
@Slf4j(topic = "graph.feat.obj.svc")
public class FileServices {

    public record FileAccessResult(Path file, Flux<DataBuffer> content) {
    }

    private final FilePathResolver filePathResolver;

    private final ValueServices valueServices;

    private final EntityServices entityServices;
    private final SchemaServices schemaServices;

    private final IdentifierFactory identifierFactory;

    private final DefaultDataBufferFactory dataBufferFactory;

    public FileServices(FilePathResolver filePathResolver, ValueServices valueServices, EntityServices entityServices, SchemaServices schemaServices, IdentifierFactory identifierFactory) {
        this.filePathResolver = filePathResolver;
        this.valueServices = valueServices;
        this.entityServices = entityServices;
        this.schemaServices = schemaServices;
        this.identifierFactory = identifierFactory;

        dataBufferFactory = new DefaultDataBufferFactory();
    }

    public Mono<RdfTransaction> store(String entityKey, Flux<DataBuffer> bytes, String prefixedPoperty, String filename, @Nullable String language, Authentication authentication) {
        if (!Authorities.satisfies(Authorities.CONTRIBUTOR, authentication.getAuthorities())) {
            String msg = String.format("Required authority '%s' for file storage not met in authentication with authorities '%s'", Authorities.CONTRIBUTOR, authentication.getAuthorities());
            return Mono.error(new InsufficientAuthenticationException(msg));
        }

        return Mono.zip(
                        entityServices.resolveAndVerify(entityKey, authentication),
                        schemaServices.resolvePrefixedName(prefixedPoperty),
                        filePathResolver.resolvePath(entityKey, filename, language)
                )
                .map(pair -> {
                    IRI entityId = pair.getT1();
                    IRI propertyIRI = pair.getT2();
                    Path path = pair.getT3();
                    LocalIdentifier fileId = this.identifierFactory.createReproducibleIdentifier(Local.Entities.NS, entityKey, filename);
                    return new LocalStorageDetails(fileId, path, entityId, propertyIRI, filename, language);
                })
                .flatMap(localStorageDetails -> DataBufferUtils.write(bytes, localStorageDetails.getPath(), StandardOpenOption.CREATE).then(Mono.just(localStorageDetails)))
                .map(localStorageDetails -> localStorageDetails.setDetails(localStorageDetails.getPath().toFile()))
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
                            .add(SDO.CONTENT_URL, vf.createLiteral(sd.getURI()))
                            .add(SDO.SUBJECT_OF, sd.getEntityId());

                    if (StringUtils.hasLength(sd.getLanguage())) {
                        builder.add(SDO.IN_LANGUAGE, vf.createLiteral(sd.getLanguage()));
                    }


                    return valueServices.insertEmbedded(sd.getEntityId(), sd.getProperty(), sd.getIdentifier(), builder.build(), authentication);
                });

    }


    public Flux<DataBuffer> read(String entityKey, String prefixedKey, String filename, @Nullable String language, Authentication authentication) {
        if (!Authorities.satisfies(Authorities.READER, authentication.getAuthorities())) {
            String msg = String.format("Required authority '%s' for file storage not met in authentication with authorities '%s'", Authorities.CONTRIBUTOR, authentication.getAuthorities());
            return Flux.error(new InsufficientAuthenticationException(msg));
        }

        return filePathResolver.resolvePath(entityKey, filename, language)
                .flatMapMany(path -> DataBufferUtils.read(path, dataBufferFactory, 4096, StandardOpenOption.READ));
    }

    public Mono<FileAccessResult> read(String contentKey, Authentication authentication) {
        if (!Authorities.satisfies(Authorities.READER, authentication.getAuthorities())) {
            String msg = String.format("Required authority '%s' for file storage not met in authentication with authorities '%s'", Authorities.CONTRIBUTOR, authentication.getAuthorities());
            throw new InsufficientAuthenticationException(msg);
        }
        return entityServices.resolveAndVerify(contentKey, authentication)
                .flatMap(contentId -> {
                    return Mono.zip(
                            this.entityServices.get(contentId, authentication),
                            Mono.just(contentId)
                    );
                }).flatMap(pair -> {
                    RdfEntity embedded = pair.getT1();
                    IRI contentID = pair.getT2();

                    IRI entityId = embedded.findDistinctValue(contentID, SDO.SUBJECT_OF).filter(Value::isIRI).map(value -> (IRI) value).orElseThrow();
                    String name = embedded.findDistinctValue(contentID, SDO.NAME).filter(Value::isLiteral).map(value -> (Literal) value).map(Value::stringValue).orElseThrow();
                    String lang = embedded.findDistinctValue(contentID, SDO.IN_LANGUAGE).filter(Value::isLiteral).map(value -> (Literal) value).map(Value::stringValue).orElse("");
                    return filePathResolver.resolvePath(entityId.getLocalName(), name, lang);
                })
                .doOnNext(path -> log.debug("Loading file with id '{}' stored in path '{}'", contentKey, path.toUri()))
                .map(path -> {
                    Flux<DataBuffer> content = DataBufferUtils.read(path, dataBufferFactory, 4096, StandardOpenOption.READ).switchIfEmpty(Mono.error(new FileNotFoundException()));
                    return new FileAccessResult(path.getFileName(), content);
                })
                .doOnError(error -> log.warn("Failed to open file with id '{}' due to error: {}", contentKey, error.getMessage()))
                .doOnSuccess(fileAccessResult -> log.debug("Loading file with id '{}' was completed", contentKey));
    }
}
