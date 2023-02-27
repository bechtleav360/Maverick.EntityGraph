package io.av360.maverick.graph.api.controller.queries;

import io.av360.maverick.graph.api.controller.AbstractController;
import io.av360.maverick.graph.model.rdf.NamespaceAwareStatement;
import io.av360.maverick.graph.services.QueryServices;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.query.BindingSet;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping(path = "/api/query")
@Slf4j(topic = "graph.api.queries")
@SecurityRequirement(name = "api_key")
public class Queries extends AbstractController {
    protected final QueryServices queryServices;

    public Queries(QueryServices queryServices) {
        this.queryServices = queryServices;
    }

    @PostMapping(value = "/select", consumes = "text/plain", produces = {"text/csv", "application/sparql-results+json"})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Sparql Query",
            content = @Content(examples = {
                    @ExampleObject(name = "Select types", value = "SELECT ?entity  ?type WHERE { ?entity a ?type } LIMIT 100"),
                    @ExampleObject(name = "Query everything", value = "SELECT ?a ?b ?c  ?type WHERE { ?a ?b ?c } LIMIT 100")
            })
    )
    @ResponseStatus(HttpStatus.ACCEPTED)
    Flux<BindingSet> queryBindings(@RequestBody String query) {

        return getAuthentication()
                .flatMapMany(authentication -> queryServices.queryValues(query, authentication))
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled()) log.debug("Search graph with tuples query: {}", query);
                });
    }


    @PostMapping(value = "/construct", consumes = "text/plain", produces = {"text/turtle", "application/ld+json"})
    @ResponseStatus(HttpStatus.ACCEPTED)
    Flux<NamespaceAwareStatement> queryStatements(@RequestBody String query) {

        return getAuthentication()
                .flatMapMany(authentication -> queryServices.queryGraph(query, authentication))
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled()) log.debug("Search graph with construct query: {}", query);
                });

    }
}
