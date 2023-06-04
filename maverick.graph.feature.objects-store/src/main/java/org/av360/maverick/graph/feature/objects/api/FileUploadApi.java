package org.av360.maverick.graph.feature.objects.api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.api.controller.AbstractController;
import org.av360.maverick.graph.feature.objects.domain.FileServices;
import org.av360.maverick.graph.model.api.ContentApi;
import org.av360.maverick.graph.model.enums.RdfMimeTypes;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.store.rdf.fragments.TripleModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@Qualifier("EntityApi")
@Order(1)
@RequestMapping(path = "")
@Slf4j(topic = "graph.feat.obj.ctrl")
@OpenAPIDefinition(

)
@SecurityRequirement(name = "api_key")
@Tag(name = "Annotations")
public class FileUploadApi extends AbstractController implements ContentApi {


    protected final FileServices fileServices;



    public FileUploadApi(FileServices fileServices) {
        this.fileServices = fileServices;
    }

    @Override
    @GetMapping(value = "/content/{id:[\\w|\\d|\\-|\\_]+}")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ResponseEntity<Flux<DataBuffer>>> download(@PathVariable String id) {
        return super.getAuthentication()
                .flatMap(authentication -> fileServices.read(id, authentication))
                .map(fileAccessResult -> {
                    ResponseEntity.BodyBuilder result = ResponseEntity.ok();

                    try {
                        String mediaType = Files.probeContentType(Path.of(fileAccessResult.file()));
                        if(! StringUtils.hasLength(mediaType)) throw new IOException();
                        result.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileAccessResult.file().toString() + "\"");
                        result.contentType(MediaType.parseMediaType(mediaType+"; charset=utf-8"));
                    } catch (IOException e) {
                        result.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileAccessResult.file().toString() + "\"");
                        result.contentType(MediaType.APPLICATION_OCTET_STREAM); 
                    }

                    return result.body(fileAccessResult.content());

                })
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled())
                        log.debug("Request to to download content with id '{}'", id);
                });
    }

    @Override
    @PostMapping(value = "/api/entities/{id:[\\w|\\d|\\-|\\_]+}/values/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d|\\-|\\_]+}",
            consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE,
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public Flux<AnnotatedStatement> createValueWithFile(
            @PathVariable String id,
            @PathVariable String prefixedKey,
            @Nullable @RequestParam(required = false) String lang,
            @RequestParam(required = false) String filename,
            @RequestBody @Parameter(name = "data", description = "The object data.") Flux<DataBuffer> bytes
    ) {
        Assert.isTrue(StringUtils.hasLength(filename), "You have to provide a filename when uploading an object.");
        Assert.isTrue(filename.split("\\.").length > 1, "The filename has to have a file extension");

        return super.getAuthentication()
                .flatMap(authentication ->
                        fileServices.store(id, bytes, prefixedKey, filename, lang, authentication)
                )
                .flatMapIterable(TripleModel::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled())
                        log.debug("Request to set property '{}' of entity '{}' to file with name '{}'", prefixedKey, id, filename);
                });
    }
}
