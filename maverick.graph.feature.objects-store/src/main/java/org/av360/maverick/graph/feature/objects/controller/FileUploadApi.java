package org.av360.maverick.graph.feature.objects.controller;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.api.controller.AbstractController;
import org.av360.maverick.graph.api.controller.ContentApi;
import org.av360.maverick.graph.feature.objects.services.FileServices;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.model.rdf.Triples;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@Qualifier("EntityApi")
@Order(1)

@Slf4j(topic = "graph.feat.obj.ctrl")


public class FileUploadApi extends AbstractController implements ContentApi {


    protected final FileServices fileServices;


    public FileUploadApi(FileServices fileServices) {
        this.fileServices = fileServices;
    }

    @Override
    public Mono<ResponseEntity<Flux<DataBuffer>>> download(@PathVariable String key) {
        return super.acquireContext()
                .flatMap(ctx -> fileServices.read(key, ctx))
                .map(fileAccessResult -> {
                    ResponseEntity.BodyBuilder result = ResponseEntity.ok();

                    try {
                        String mediaType = Files.probeContentType(Path.of(fileAccessResult.filename()));
                        if (!StringUtils.hasLength(mediaType)) throw new IOException();
                        result.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileAccessResult.file().toString() + "\"");
                        result.contentType(MediaType.parseMediaType(mediaType + "; charset=utf-8"));
                    } catch (IOException e) {
                        result.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileAccessResult.file().toString() + "\"");
                        result.contentType(MediaType.APPLICATION_OCTET_STREAM);
                    }

                    return result.body(fileAccessResult.content());

                })
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled())
                        log.debug("Request to to download content with id '{}'", key);
                });
    }

    @Override
    public Flux<AnnotatedStatement> createValueWithFile(
            String key,
            String prefixedProperty,
            String languageTag,
            String filename,
            Flux<DataBuffer> bytes
    ) {
        Assert.isTrue(StringUtils.hasLength(filename), "You have to provide a filename when uploading an object.");
        Assert.isTrue(filename.split("\\.").length > 1, "The filename has to have a file extension");

        return super.acquireContext()
                .flatMap(ctx ->
                        fileServices.store(key, bytes, prefixedProperty, filename, languageTag, ctx)
                )
                .flatMapIterable(Triples::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled())
                        log.debug("Request to set property '{}' of entity '{}' to file with name '{}'", prefixedProperty, key, filename);
                });
    }
}
