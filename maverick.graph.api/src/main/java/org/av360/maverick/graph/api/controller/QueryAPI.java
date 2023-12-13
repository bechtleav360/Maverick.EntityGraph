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

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.model.enums.SparqlMimeTypes;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.eclipse.rdf4j.query.BindingSet;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

public interface QueryAPI {
    @PostMapping(value = "/select", consumes = {MediaType.TEXT_PLAIN_VALUE, SparqlMimeTypes.SPARQL_QUERY_VALUE},
            produces = {
                    SparqlMimeTypes.JSON_VALUE,
                    "text/csv; charset=utf-8"})
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

    @PostMapping(value = "/query", consumes = {MediaType.TEXT_PLAIN_VALUE, SparqlMimeTypes.SPARQL_QUERY_VALUE},
            produces = { SparqlMimeTypes.JSON_VALUE, SparqlMimeTypes.CSV_VALUE}
    )

    @GetMapping(value = "/select", produces = {SparqlMimeTypes.CSV_VALUE, SparqlMimeTypes.JSON_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<BindingSet> queryBindingsGet(@RequestParam(required = true) String query,
                                      @RequestParam(required = false, defaultValue = "entities", value = "entities") @Parameter(name = "repository", description = "The repository type in which the query should search.")
                                      RepositoryType repositoryType);

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
