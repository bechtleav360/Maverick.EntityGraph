package org.av360.maverick.graph.model.api;

import io.swagger.v3.oas.annotations.Parameter;
import org.av360.maverick.graph.model.enums.RdfMimeTypes;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;

public interface ContentApi {
    @GetMapping(value = "/content/{id:[\\w|\\d|\\-|\\_]+}")
    @ResponseStatus(HttpStatus.OK)
    Mono<ResponseEntity<Flux<DataBuffer>>> download(@PathVariable String id);

    @PostMapping(value = "/api/entities/{id:[\\w|\\d|\\-|\\_]+}/values/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d|\\-|\\_]+}",
            consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE,
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<AnnotatedStatement> createValueWithFile(
            @PathVariable String id,
            @PathVariable String prefixedKey,
            @Nullable @RequestParam(required = false) String lang,
            @RequestParam(required = false) String filename,
            @RequestBody @Parameter(name = "data", description = "The object data.") Flux<DataBuffer> bytes
    );
}
