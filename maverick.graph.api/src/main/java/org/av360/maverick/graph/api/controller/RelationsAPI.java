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
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import javax.annotation.Nullable;

@RequestMapping(path = "/api")
@SecurityRequirement(name = "api_key")
@Tag(name = "Relations",
        description = """
                ### Manage relations for entities
                
                Methods to manage relations between entities. They enable reading and updating all relations associated
                with an entity. An entity relation represents a connection between two entities.
                The related entities have to be managed by an entity graph instance (not necessarily the current instance).
                For linking to external resources, one can leverage the values API.
                \s
                Relations are auto-removed if either of the linked entities ceases to exist. Relations are treated are
                entity properties (as are values) and can be annotated with details. 
                """)
public interface RelationsAPI {


    @Operation(
            operationId = "listRelations",
            summary = "Returns all relations of an entity.",
            description = """
                    Returns all relations defined for the given entity.
                    """,
            parameters = {
                    @Parameter(name = "key",description = "Unique identifier for the entity", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string")),
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully return the list of relations", content = @Content(schema = @Schema(implementation = AnnotatedStatement.class))),
                    @ApiResponse(responseCode = "404", description = "Entity with specified ID not found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorAttributes.class))}),
                    @ApiResponse(responseCode = "400", description = "Invalid request parameters", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorAttributes.class))})
            }
    )
    @GetMapping(value = "/entities/{key:[\\w|\\d|\\-|\\_]+}/relations",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<AnnotatedStatement> list(@PathVariable String key);


    @Operation(
            operationId = "listRelationsByType",
            summary = "Returns all relations for the same property.",
            description = """
                    Returns all relations defined for the given entity sharing the same predicate.
                    """,
            parameters = {
                    @Parameter(name = "key", description = "Unique identifier for the entity", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string")),
                    @Parameter(name = "prefixedProperty", description = "Prefixed property for the entity relation", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string")),
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully return the list of relations", content = @Content(schema = @Schema(implementation = AnnotatedStatement.class))),
                    @ApiResponse(responseCode = "404", description = "Entity with specified ID not found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorAttributes.class))}),
                    @ApiResponse(responseCode = "400", description = "Invalid request parameters", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorAttributes.class))})
            }
    )
    @GetMapping(value = "/entities/{key:[\\w|\\d|\\-|\\_]+}/relations/{prefixedProperty:[\\w|\\d]+\\.[\\w|\\d]+}",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<AnnotatedStatement> getLinksByType(@PathVariable String key, @PathVariable String prefixedProperty);


    @Operation(
            operationId = "insertRelation",
            summary = "Create edge to existing entity identified by target id (within the same dataset).",
            description = """
                    Creates an edge to an existing entity within the same graph (application) identified by the target identifier to link the two entities. An edge represents a relationship between 
                    two nodes in the graph, while the node represents an entity in the graph. When creating an edge to an existing entity, the target identifier
                    is used to identify the specific entity to which the edge will be created. The creation of the edge fails, if no entity with the target identifier exists.      
                    Multiple edges can connect the same two nodes, representing different relationships between the entities they represent. For example, in a social network,
                    two users can be connected by multiple edges representing different types of relationships, such as friendship, family, or work-related connections.
                                        
                    To create a link to an entity within a different application (or externally managed entity), use the values api instead. 
                    """,
            parameters = {
                    @Parameter(name = "key", description = "Unique identifier for the entity", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string")),
                    @Parameter(name = "prefixedProperty", description = "Prefixed property for the entity relation", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string")),
                    @Parameter(name = "targetKey", description = "Unique identifier for the target entity", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string")),

            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully return the list of relations", content = @Content(schema = @Schema(implementation = AnnotatedStatement.class))),
                    @ApiResponse(responseCode = "404", description = "Entity with specified ID not found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorAttributes.class))}),
                    @ApiResponse(responseCode = "400", description = "Invalid request parameters", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorAttributes.class))})
            }
    )
    @PutMapping(value = "/entities/{key:[\\w|\\d|\\-|\\_]+}/relations/{prefixedProperty:[\\w|\\d]+\\.[\\w|\\d]+}/{targetKey:[\\w|\\d|\\-|\\_]+}",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.CREATED)
    Flux<AnnotatedStatement> insert(@PathVariable String key, @PathVariable String prefixedProperty, @PathVariable String targetKey, @Nullable @RequestParam(required = false) Boolean replace);

    @Operation(summary = "Deletes edge to existing entity identified by target id.")
    @DeleteMapping(value = "/entities/{key:[\\w|\\d|\\-|\\_]+}/relations/{prefixedProperty:[\\w|\\d]+\\.[\\w|\\d]+}/{targetKey:[\\w|\\d|-|_]+}",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<AnnotatedStatement> deleteLink(@PathVariable String key, @PathVariable String prefixedProperty, @PathVariable String targetKey);

    @Operation(summary = "Returns all links of the given type.", hidden = true)
    @PutMapping(value = "/entities/{key:[\\w|\\d|\\-|\\_]+}/relations/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<AnnotatedStatement> batchCreateLinks(@PathVariable String key, @PathVariable String prefixedProperty);
}
