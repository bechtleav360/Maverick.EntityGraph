package com.bechtle.eagl.graph.api.controller;

import com.bechtle.eagl.graph.domain.model.extensions.NamespaceAwareStatement;
import com.bechtle.eagl.graph.domain.services.QueryServices;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(path = "/api/query")
@Api(tags = "Queries")
@Slf4j
public class Queries {
    protected final QueryServices queryServices;

    public Queries(QueryServices queryServices) {
        this.queryServices = queryServices;
    }

    @ApiOperation(value = "Run a query", tags = {"v1"})
    @PostMapping(value = "/select", consumes = "text/plain", produces = {"text/csv", "application/sparql-results+json"})
    @ResponseStatus(HttpStatus.ACCEPTED)
    Mono<TupleQueryResult> queryBindings(@RequestBody String query) {
        log.trace("(Request) Search graph with tuples query: {}", query.toString());
        return queryServices.queryValues(query); // .map(ResponseEntity::ok);
    }


    @ApiOperation(value = "Run a query", tags = {"v1"})
    @PostMapping(value = "/construct", consumes = "text/plain", produces = {"text/turtle", "application/ld+json"})
    @ResponseStatus(HttpStatus.ACCEPTED)
    Flux<NamespaceAwareStatement> queryStatements(@RequestBody String query) {
        log.trace("(Request) Search graph with construct query: {}", query.toString());

        return queryServices.queryGraph(query);
    }
}
