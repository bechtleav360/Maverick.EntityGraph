/*
 * Copyright (c) 2023.
 *
 *  Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 *  European Commission - subsequent versions of the EUPL (the "Licence");
 *
 *  You may not use this work except in compliance with the Licence.
 *  You may obtain a copy of the Licence at:
 *
 *  https://joinup.ec.europa.eu/software/page/eupl5
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the Licence for the specific language governing permissions and limitations under the Licence.
 *
 */

package org.av360.maverick.graph.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@RequestMapping(path = "")
@SecurityRequirement(name = "api_key")
@Tag(name = "Values")
public interface ContentApi {
    @GetMapping(value = "/content/{id:[\\w|\\d|\\-|\\_]+}")
    @ResponseStatus(HttpStatus.OK)
    Mono<ResponseEntity<Flux<DataBuffer>>> download(@PathVariable String key);

    @Operation(
            operationId = "uploadContent",
            summary = "Upload a file as content for a value property.",
            description = """
                    This method expects a file upload. The file will be stored as binary in an object store and linked as 
                    embedded value object to the entity. You can access the file via its content identifier. 
                                        """,
            parameters = {
                    @Parameter(name = "key", description = "Unique identifier for the entity", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string")),
                    @Parameter(name = "prefixedProperty", description = "Prefixed property for the entity value", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string")),
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully stored the content as binary object", content = @Content(mediaType = RdfMimeTypes.TURTLESTAR_VALUE, schema = @Schema(implementation = AnnotatedStatement.class)))
            }
    )
    @PostMapping(value = "/api/entities/{key:[\\w|\\d|\\-|\\_]+}/objects/{prefixedProperty:[\\w|\\d]+\\.[\\w|\\d|\\-|\\_]+}",
            consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE,
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<AnnotatedStatement> createValueWithFile(
            @PathVariable String key,
            @PathVariable String prefixedProperty,
            @Nullable @RequestParam(required = false) String languageTag,
            @RequestParam(required = false) String filename,
            @RequestBody @Parameter(name = "data", description = "The object data.") Flux<DataBuffer> bytes
    );
}
