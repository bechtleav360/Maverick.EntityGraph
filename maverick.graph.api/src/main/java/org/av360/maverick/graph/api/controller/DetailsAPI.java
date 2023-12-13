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
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.model.enums.PropertyType;
import org.av360.maverick.graph.model.enums.RdfMimeTypes;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RequestMapping(path = "/api")
@SecurityRequirement(name = "api_key")
@Tag(name = "Details",
        description = """
                ### Manage details for values and relations
                \s
                Methods to add and remove details as annotations for values and relations. These details can be used
                to add additional metadata such as its provenance (e.g. source, modification dates, etc.) or credibility
                (e.g. uncertainty, applied technology to create the value, model type, etc.). 
                \s
                Details are stored as statements about statements ([RDF-Star](https://w3c.github.io/rdf-star/cg-spec/editors_draft.html)) in the graph and can queried in SPARQL. All
                reading operations require a parser which understands [quoted triples](https://w3c.github.io/rdf-turtle/spec/#quoted-triples) in "Turtle-star". 
                
                
                """)
public interface DetailsAPI {
    @Schema(
            example = """
                    [
                        {
                            "property": "urn:pwid:annot:source",
                            "value": "mistral"
                        }
                    ]
                    """
    )
    public record Detail(String property, String value) {
    }

    @Operation(
            operationId = "listDetails",
            summary = "Returns all details for a value or relation",
            description = """
                    Returns a list of details for a given value
                    """,
            parameters = {
                    @Parameter(name = "key", description = "Unique identifier for the entity", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string")),
                    @Parameter(name = "type", description = "Add a detail either for a value or a relation", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string", allowableValues = {"relations", "values"})),
                    @Parameter(name = "prefixedProperty", description = "Prefixed property for the entity value", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string")),
                    @Parameter(name = "valueIdentifier", description = "Identifier for the specific value", required = false, in = ParameterIn.QUERY, schema = @Schema(type = "string"))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully listed the details", content = @Content(schema = @Schema(implementation = Transaction.class))),
                    @ApiResponse(responseCode = "404", description = "Entity with specified ID not found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorAttributes.class))}),
                    @ApiResponse(responseCode = "400", description = "Invalid request parameters", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorAttributes.class))})
            }

    )
    @GetMapping(value = "/entities/{key:[\\w|\\d|\\-|\\_]+}/{type}/{prefixedProperty:[\\w|\\d]+\\.[\\w|\\d]+}/details",
            produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<Detail> getDetails(
            @PathVariable @Parameter(name = "entity identifier") String key,
            @PathVariable PropertyType type,
            @PathVariable String prefixedProperty,
            @RequestParam String valueIdentifier
    );


    @Operation(
            operationId = "removeDetail",
            summary = "Delete a specific detail for a value",
            description = """
                    **Removes a detail for the given value.**
                    \s 
                    Use the language tag or value identifier to select a specific value (if multiple values exist for a property). 
                    """,
            parameters = {
                    @Parameter(name = "key", description = "Unique identifier for the entity", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string")),
                    @Parameter(name = "type", description = "Add a detail either for a value or a relation", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string", allowableValues = {"relations", "values"})),
                    @Parameter(name = "prefixedProperty", description = "Prefixed property for the entity value", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string")),
                    @Parameter(name = "prefixedDetailProperty", description = "Prefixed property for the detail statement", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string")),
                    @Parameter(name = "valueIdentifier", description = "Identifier for the specific value", required = false, in = ParameterIn.QUERY, schema = @Schema(type = "string"))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully deleted the detail", content = @Content(schema = @Schema(implementation = Transaction.class))),
                    @ApiResponse(responseCode = "404", description = "Entity with specified ID not found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorAttributes.class))}),
                    @ApiResponse(responseCode = "400", description = "Invalid request parameters", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorAttributes.class))})
            }
    )
    @DeleteMapping(value = "/entities/{key:[\\w|\\d|\\-|\\_]+}/{type}/{prefixedProperty:[\\w|\\d]+\\.[\\w|\\d]+}/details/{prefixedDetailProperty:[\\w|\\d]+\\.[\\w|\\d]+}",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<AnnotatedStatement> remove(
            @PathVariable String key,
            @PathVariable PropertyType type,
            @PathVariable String prefixedProperty,
            @PathVariable String prefixedDetailProperty,
            @RequestParam(required = false) String valueIdentifier);

    @Operation(
            operationId = "insertDetail",
            summary = "Adds a detail for a value or relation.",
            description = """
                    **Creates a detail with the given property for a value or relation property.**
                    \s 
                    Existing details of the same type are always replaced. Use the language tag or value identifier to 
                    select a specific value (if multiple values exist for a property). 
                    """,
            parameters = {
                    @Parameter(name = "key", description = "Unique identifier for the entity", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string")),
                    @Parameter(name = "type", description = "Add a detail either for a value or a relation", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string", allowableValues = {"relations", "values"})),
                    @Parameter(name = "prefixedProperty", description = "Prefixed property for the entity value", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string")),
                    @Parameter(name = "prefixedDetailProperty", description = "Prefixed property for the detail statement", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string")),
                    @Parameter(name = "valueIdentifier", description = "Identifier for the specific value", required = false, in = ParameterIn.QUERY, schema = @Schema(type = "string"))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully set the detail for the value or link property", content = @Content(schema = @Schema(implementation = AnnotatedStatement.class))),
                    @ApiResponse(responseCode = "404", description = "Entity with specified ID not found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorAttributes.class))}),
                    @ApiResponse(responseCode = "400", description = "Invalid request parameters", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorAttributes.class))})
            }
    )
    @PostMapping(value = "/entities/{key:[\\w|\\d|\\-|\\_]+}/{type}/{prefixedProperty:[\\w|\\d]+\\.[\\w|\\d]+}/details/{prefixedDetailProperty:[\\w|\\d]+\\.[\\w|\\d]+}",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<AnnotatedStatement> insert(
            @PathVariable String key,
            @PathVariable PropertyType type,
            @PathVariable String prefixedProperty,
            @PathVariable String prefixedDetailProperty,
            @RequestParam(required = false) String valueIdentifier,
            @RequestBody String value
            );


}
