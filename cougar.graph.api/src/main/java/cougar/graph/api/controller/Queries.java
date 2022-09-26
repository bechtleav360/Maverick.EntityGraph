package cougar.graph.api.controller;

import cougar.graph.model.rdf.NamespaceAwareStatement;
import cougar.graph.services.services.QueryServices;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.query.BindingSet;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping(path = "/api/query")
@Api(tags = "Queries")
@Slf4j(topic = "graph.api.queries")
public class Queries extends AbstractController {
    protected final QueryServices queryServices;

    public Queries(QueryServices queryServices) {
        this.queryServices = queryServices;
    }

    @ApiOperation(value = "Run a query")
    @PostMapping(value = "/select", consumes = "text/plain", produces = {"text/csv", "application/sparql-results+json"})
    @ResponseStatus(HttpStatus.ACCEPTED)
    Flux<BindingSet> queryBindings(@RequestBody String query) {

        return getAuthentication()
                .flatMapMany(authentication -> queryServices.queryValues(query, authentication))
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled()) log.debug("Search graph with tuples query: {}", query);
                });
    }


    @ApiOperation(value = "Run a query")
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
