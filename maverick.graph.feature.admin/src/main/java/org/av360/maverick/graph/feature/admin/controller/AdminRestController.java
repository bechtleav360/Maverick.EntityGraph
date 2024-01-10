package org.av360.maverick.graph.feature.admin.controller;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.api.controller.AbstractController;
import org.av360.maverick.graph.feature.admin.controller.dto.ImportFromEndpointRequest;
import org.av360.maverick.graph.feature.admin.services.AdminServices;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.store.rdf.helpers.RdfUtils;
import org.eclipse.rdf4j.rio.RDFParserFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

@RestController
@Slf4j(topic = "graph.feat.admin.ctrl.api")
public class AdminRestController extends AbstractController implements org.av360.maverick.graph.feature.admin.AdminWebApi {
    protected final AdminServices adminServices;

    public AdminRestController(AdminServices adminServices) {
        this.adminServices = adminServices;
    }

    @Override
    public Mono<Void> resetRepository(RepositoryType repositoryType) {
        Assert.notNull(repositoryType, "Invalid value for repository type: " + repositoryType);

        return super.acquireContext()
                .map(context -> context.updateEnvironment(env -> env.withRepositoryType(repositoryType)))
                .flatMap(adminServices::reset)
                .doOnError(throwable -> log.error("Error while purging repository. Type '{}' with reason: {}", throwable.getClass().getSimpleName(), throwable.getMessage()))
                .doOnSubscribe(s -> log.info("Request to empty the repository of type '{}'", repositoryType));
    }


    @Override
    public Mono<Void> importEntities(
            Flux<DataBuffer> bytes,
            RepositoryType repositoryType,
            String mimetype
    ) {
        Assert.isTrue(StringUtils.hasLength(mimetype), "Mimetype is a required parameter");

        return super.acquireContext()
                .map(context -> context.updateEnvironment(env -> env.setRepositoryType(repositoryType)))
                .flatMap(ctx -> adminServices.importEntities(bytes, mimetype, ctx))
                .doOnError(throwable -> log.error("Error while importing to repository.", throwable))
                .doOnSubscribe(s -> log.debug("Request to import a request of mimetype {}", mimetype));
    }

    @Override
    public Mono<Void> importFromSparql(
            ImportFromEndpointRequest importFromEndpointRequest,
            RepositoryType repositoryType
    ) {

        return super.acquireContext()
                .map(context -> context.updateEnvironment(env -> env.setRepositoryType(repositoryType)))
                .flatMap(ctx -> adminServices.importFromEndpoint(importFromEndpointRequest.endpoint(), importFromEndpointRequest.headers(), 1000, 0, ctx))
                .doOnError(throwable -> log.error("Error while importing to repository.", throwable))
                .doOnSubscribe(s -> log.debug("Request to import a request from endpoint {}", importFromEndpointRequest.endpoint()));
    }

    @Override
    public Mono<Void> importFile(
            Mono<FilePart> fileMono,
            RepositoryType repositoryType,
            String mimetype) {
        Assert.isTrue(StringUtils.hasLength(mimetype), "Mimetype is a required parameter");

        Optional<RDFParserFactory> parserFactory = RdfUtils.getParserFactory(MimeType.valueOf(mimetype));
        Assert.isTrue(parserFactory.isPresent(), "Unsupported mimetype for parsing the file. Supported mimetypes are: " + RdfUtils.getSupportedMimeTypes());

        return super.acquireContext()
                .map(context -> context.getEnvironment().withRepositoryType(repositoryType))
                .flatMap(context -> Mono.zip(Mono.just(context), fileMono))
                .flatMap(pair -> adminServices.importEntities(pair.getT2().content(), mimetype, pair.getT1()))
                .doOnError(throwable -> log.error("Error while importing to repository.", throwable))
                .doOnSubscribe(s -> log.info("Request to import a file of mimetype {}", mimetype));
    }

    @Override
    public Mono<Void> importPackage(
            Mono<FilePart> fileMono,
            RepositoryType repositoryType) {

        return super.acquireContext()
                .map(context -> context.getEnvironment().withRepositoryType(repositoryType))
                .flatMap(context -> Mono.zip(Mono.just(context), fileMono))
                .flatMap(pair -> adminServices.importPackage(pair.getT2(), pair.getT1()))
                .doOnError(throwable -> log.error("Error while importing package to repository.", throwable))
                .doOnSubscribe(s -> log.info("Request to import a packaged file"));
    }

}
