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
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.av360.maverick.graph.api.controller.dto.Responses;
import org.av360.maverick.graph.model.enums.RdfMimeTypes;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.model.rdf.Triples;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;




@SecurityRequirement(name = "api_key")
@Tag(name = "Entities", description = """
        ### Manage entities in the Entity Graph.
        \s
        An **entity** represents a foundational concept in this API. An entity is fundamentally a set of statements that
        share the same subject, forming a cohesive unit centered around that subject.
        \s
        Entities comprise two primary components:
        * **Values**, which are specific data points or information associated with the entity, and 
        * **Relations**, representing the connections or relationships of the entity to other entities of classifiers. 
        \s
        Every entity is **strongly typed**, ensuring adherence to a designated structure or category. In addition, every 
        entity gets assigned a unique identifier, the Entity Key. 
        """,
        extensions = @Extension(name = "order", properties = {@ExtensionProperty(name = "position", value = "1")}))
@RequestMapping(path = "/api")
public interface EntitiesAPI {



    @Operation(operationId = "readEntity",
            summary = "Returns an entity with the given unique identifier.",
            description = """
                    This operation retrieves an Entity using a provided unique Entity Key. The response includes the entity,
                    its type definition, all associated values, and relations to other entities. It also incorporates embedded
                    fragments, specific sets of statements inherent to the entity. However, details or value identifiers are
                    omitted due to their reliance on RDF-Star encodings, which are not supported by JSON-LD.
                     """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved entity details"),
                    @ApiResponse(responseCode = "404", description = "Entity with specified ID not found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorAttributes.class))}),
                    @ApiResponse(responseCode = "400", description = "Invalid request parameters", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorAttributes.class))})
            }
    )
    @GetMapping(value = "/entities/{id}",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<AnnotatedStatement> readAsRDF(@Parameter(description = "Key of the entity to be fetched", required = true) @PathVariable String key,
                                  @Parameter(description = "Prefixed property key like 'dc.identifier' pointing to a global external identifier, if the internal key is unknown.", required = false) @RequestParam(required = false) @Nullable String property);

    @GetMapping(value = "/entities/{id}",
            produces = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLESTAR_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<AnnotatedStatement> readAsRDFStar(@Parameter(description = "Key of the entity to be fetched", required = true) @PathVariable String key,
                                       @Parameter(description = "Prefixed property key like 'dc.identifier' pointing to a global external identifier, if the internal key is unknown.", required = false) @RequestParam(required = false) @Nullable String property);

    @GetMapping(value = "/entities/{id}",
            produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Mono<Responses.EntityResponse> readAsItem(@Parameter(description = "Key of the entity to be fetched", required = true) @PathVariable String key,
                                              @Parameter(description = "Prefixed property key like 'dc.identifier' pointing to a global external identifier, if the internal key is unknown.", required = false) @RequestParam(required = false) @Nullable String property);

    @GetMapping(value = "/entities", produces = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE})
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "List Entities",
            description = """
                    Fetch a list of entities based on the provided limit and offset parameters. An entity in this API
                    is a central unit made up of statements with a common subject. It consists of Values (specific data
                    about the entity) and Relations (links to other entities or classifiers). Each entity has a strict
                    type. Responses usually provide a summarized view without detailed values.
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successful retrieval of entities list",
                            content = @Content(schema = @Schema(implementation = AnnotatedStatement.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request parameters",
                            content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorAttributes.class))}),
                    @ApiResponse(responseCode = "500", description = "Server error",
                            content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorAttributes.class))}),
            })
    Flux<AnnotatedStatement> list(
            @Parameter(description = "Limit on the number of entities to retrieve. Default is 100.", required = false)
            @RequestParam(value = "limit", defaultValue = "100") Integer limit,

            @Parameter(description = "Offset for pagination. Default is 0.", required = false)
            @RequestParam(value = "offset", defaultValue = "0") Integer offset);

    @PostMapping(value = "/entities",
            consumes = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE, RdfMimeTypes.NTRIPLES_VALUE},
            produces = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE, RdfMimeTypes.NTRIPLES_VALUE})
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Create Entity",
            description = """
                    Creates an entity from a valid RDF document in the request body. The document can contain multiple 
                    linked data fragments (statements with a common subject as selector).
                    Type definitions are mandatory for all subjects.
                    """,
            responses = {
                    @ApiResponse(responseCode = "202", description = "Entity successfully created",
                            content = @Content(schema = @Schema(implementation = AnnotatedStatement.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid RDF document or missing type definitions"),
                    @ApiResponse(responseCode = "500", description = "Server error")
            })
    Flux<AnnotatedStatement> create(@Parameter(description = "The RDF statements for the new entity.", required = true) @RequestBody Triples request);


    @DeleteMapping(value = "/entities/{id:[\\w|\\d|\\-|\\_]+}",
            produces = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE})
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Delete Entity by Key",
            description = """
                    This operation is designed to manage the removal of entities by their unique identifier.
                    On invocation, it requires a valid entity key to serve as the subject for the statements 
                    in question. Once a valid key is provided, all corresponding statements that constitute 
                    this entity will be purged from the system. It's important to note that any incoming statements 
                    pointing to the entity will not be immediately removed. Instead, the removal of such incoming 
                    statements is managed in a deferred manner, being processed as a background job to 
                    ensure system performance and integrity.
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully deleted the entity and its statements."),
                    @ApiResponse(responseCode = "400", description = "Invalid entity key supplied."),
                    @ApiResponse(responseCode = "404", description = "Entity not found."),
                    @ApiResponse(responseCode = "500", description = "Internal server error.")
            }

    )
    Flux<AnnotatedStatement> delete(@Parameter(description = "Key of the entity to be fetched", required = true) @PathVariable String key);
}
