package io.av360.maverick.graph.feature.admin.api;

import io.av360.maverick.graph.api.controller.AbstractController;
import io.av360.maverick.graph.feature.admin.domain.AdminServices;
import io.av360.maverick.graph.store.RepositoryType;
import io.av360.maverick.graph.store.rdf.helpers.RdfUtils;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.rio.RDFParserFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Locale;
import java.util.Optional;

@RestController
@RequestMapping(path = "/api/admin/bulk")
//@Api(tags = "Admin Operations")
@Slf4j(topic = "graph.feat.admin.ctrl.api")
@SecurityRequirement(name = "api_key")
public class Admin extends AbstractController {
    protected final AdminServices adminServices;

    public Admin(AdminServices adminServices) {
        this.adminServices = adminServices;
    }

    //@ApiOperation(value = "Empty repository", tags = {})
    @GetMapping(value = "/reset", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    Mono<Void> queryBindings(@RequestParam(name = "name") String repositoryTypeName) {
        RepositoryType repositoryType;
        if (!StringUtils.hasLength(repositoryTypeName))
            repositoryType = RepositoryType.ENTITIES;
        else
            repositoryType = RepositoryType.valueOf(repositoryTypeName.toUpperCase(Locale.getDefault()));

        Assert.notNull(repositoryType, "Invalid value for repository type: " + repositoryTypeName);

        return super.getAuthentication()
                .flatMap(auth -> adminServices.reset(auth, repositoryType))
                .doOnError(throwable -> log.error("Error while purging repository.", throwable))
                .doOnSubscribe(s -> log.debug("Request to empty the repository of type '{}'", repositoryType));
    }


    //@ApiOperation(value = "Import RDF into entity repository", tags = {})
    @PostMapping(value = "/import/entities", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    Mono<Void> importEntities(
            @RequestBody Flux<DataBuffer> bytes,
            // @ApiParam(example = "text/turtle")
            @RequestParam String mimetype) {
        Assert.isTrue(StringUtils.hasLength(mimetype), "Mimetype is a required parameter");

        return super.getAuthentication()
                .flatMap(authentication -> adminServices.importEntities(bytes, mimetype, authentication))
                .doOnError(throwable -> log.error("Error while importing to repository.", throwable))
                .doOnSubscribe(s -> log.debug("Request to import a request of mimetype {}", mimetype));
    }

    //@ApiOperation(value = "Import RDF file into entity repository", tags = {})
    @PostMapping(value = "/import/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    Mono<Void> importFile(
            @RequestPart Mono<FilePart> fileMono,
            //@ApiParam(example = "text/turtle")
            @RequestParam String mimetype) {
        Assert.isTrue(StringUtils.hasLength(mimetype), "Mimetype is a required parameter");

        Optional<RDFParserFactory> parserFactory = RdfUtils.getParserFactory(MimeType.valueOf(mimetype));
        Assert.isTrue(parserFactory.isPresent(), "Unsupported mimetype for parsing the file. Supported mimetypes are: " + RdfUtils.getSupportedMimeTypes());

        return Mono.zip(super.getAuthentication(), fileMono)
                .flatMap(objects -> adminServices.importEntities(objects.getT2().content(), mimetype, objects.getT1()))
                .doOnError(throwable -> log.error("Error while importing to repository.", throwable))
                .doOnSubscribe(s -> log.debug("Request to import a file of mimetype {}", mimetype));
    }


}
