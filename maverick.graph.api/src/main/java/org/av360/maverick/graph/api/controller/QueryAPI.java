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
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.model.enums.SparqlMimeTypes;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.eclipse.rdf4j.query.BindingSet;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequestMapping(path = "/api/query")
@SecurityRequirement(name = "api_key")
@Tag(name = "Queries", description = """
        ### Searching in the repositories

        Runs sparql queries. If "text/csv" is used as return type, make sure you attach the correct encoding ("text/csv; charset=utf-8") 
        to your Accept Header. 
        """,
        extensions = @Extension(name = "order", properties = {@ExtensionProperty(name = "position", value = "1")}))
public interface QueryAPI {

    @Operation(operationId = "selectWithPost",
            summary = "Runs a select query.",
            description = """
                    Runs a query (with valid sparql query in Request Body). Don't forget to add a limit. 
                     """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved entity details"),
                    @ApiResponse(responseCode = "404", description = "Entity with specified ID not found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorAttributes.class))}),
                    @ApiResponse(responseCode = "400", description = "Invalid request parameters", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorAttributes.class))})
            }
    )
    @PostMapping(value = "/select", consumes = {MediaType.TEXT_PLAIN_VALUE, SparqlMimeTypes.SPARQL_QUERY_VALUE},
            produces = { SparqlMimeTypes.JSON_VALUE, "text/csv; charset=utf-8"})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Sparql Select Query",
            content = @Content(examples = {
                    @ExampleObject(name = "Select types", value = "SELECT ?entity  ?type WHERE { ?entity a ?type } LIMIT 100"),
                    @ExampleObject(name = "Query everything", value = "SELECT ?a ?b ?c  ?type WHERE { ?a ?b ?c } LIMIT 100")
            })
    )
    @ResponseStatus(HttpStatus.OK)
    Flux<BindingSet> queryBindingsPost(@RequestBody String query,
                                       @RequestParam(required = false, defaultValue = "entities", value = "entities") @Parameter(name = "repository", description = "The repository type in which the query should search.")
                                       RepositoryType repositoryType);


    @PostMapping(value = "/update", consumes = {MediaType.TEXT_PLAIN_VALUE, SparqlMimeTypes.SPARQL_QUERY_VALUE},
            produces = { SparqlMimeTypes.JSON_VALUE, "text/csv; charset=utf-8"})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Sparql Update Query (Be careful!)",
            content = @Content(examples = {
                    @ExampleObject(name = "Select types", value = "SELECT ?entity  ?type WHERE { ?entity a ?type } LIMIT 100"),
                    @ExampleObject(name = "Query everything", value = "SELECT ?a ?b ?c  ?type WHERE { ?a ?b ?c } LIMIT 100")
            })
    )
    @ResponseStatus(HttpStatus.OK)
    Mono<Void> queryUpdatePost(@RequestBody String query,
                                       @RequestParam(required = false, defaultValue = "entities", value = "entities") @Parameter(name = "repository", description = "The repository type in which the query should update.")
                                       RepositoryType repositoryType);



    @Operation(operationId = "selectWithGet",
            summary = "Runs a select query.",
            description = """
                    Runs a query (with valid sparql query as parameter). Don't forget to add a limit. 
                     """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved entity details"),
                    @ApiResponse(responseCode = "404", description = "Entity with specified ID not found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorAttributes.class))}),
                    @ApiResponse(responseCode = "400", description = "Invalid request parameters", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorAttributes.class))})
            }
    )
    @GetMapping(value = "/select", produces = {SparqlMimeTypes.CSV_VALUE, SparqlMimeTypes.JSON_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<BindingSet> queryBindingsGet(@RequestParam(required = true) String query,
                                      @RequestParam(required = false, defaultValue = "entities", value = "entities") @Parameter(name = "repository", description = "The repository type in which the query should search.")
                                      RepositoryType repositoryType);

    @Operation(operationId = "constructWithPost",
            summary = "Runs a construct query.",
            description = """
                    Runs a query (with valid sparql query in request body). Don't forget to add a limit. 
                     """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved entity details"),
                    @ApiResponse(responseCode = "404", description = "Entity with specified ID not found", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorAttributes.class))}),
                    @ApiResponse(responseCode = "400", description = "Invalid request parameters", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorAttributes.class))})
            }
    )
    @PostMapping(value = "/construct", consumes = "text/plain", produces = {"text/turtle", "application/ld+json"})
    @ResponseStatus(HttpStatus.OK)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Sparql Construct Query",
            content = @Content(examples = {
                    @ExampleObject(name = "Query everything", value = "CONSTRUCT WHERE { ?s ?p ?o . } LIMIT 100")
            })
    )
    Flux<AnnotatedStatement> queryStatements(@RequestBody String query, @RequestParam(required = false, defaultValue = "entities", value = "entities") @Parameter(name = "repository", description = "The repository type in which the query should search.")
    RepositoryType repositoryType);
}
