package org.av360.maverick.graph.api.controller.queries;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.api.controller.AbstractController;
import org.av360.maverick.graph.api.controller.QueryAPI;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.services.QueryServices;
import org.eclipse.rdf4j.query.BindingSet;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController

@Slf4j(topic = "graph.ctrl.queries")
public class QueryRestController extends AbstractController implements QueryAPI {
    protected final QueryServices queryServices;

    public QueryRestController(QueryServices queryServices) {
        this.queryServices = queryServices;
    }


    @Override
    public Flux<BindingSet> queryBindingsPost(String query, RepositoryType repositoryType) {

        return super.acquireContext()
                .flatMapMany(ctx -> queryServices.queryValues(query, repositoryType, ctx))
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled()) log.debug("Request to search graph with tuples query: {}", query);
                });
    }

    @Override
    public Mono<Void> queryUpdatePost(String query, RepositoryType repositoryType) {
        return super.acquireContext()
                .flatMap(ctx -> queryServices.update(query, repositoryType, ctx))
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled()) log.debug("Request to search graph with tuples query: {}", query);
                });
    }

    @Override
    public Flux<BindingSet> queryBindingsGet(String query, RepositoryType repositoryType) {

        return super.acquireContext()
                .flatMapMany(ctx -> queryServices.queryValues(query, repositoryType, ctx))
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled()) log.debug("Request to search graph with tuples query: {}", query);
                });
    }


    @Override
    public Flux<AnnotatedStatement> queryStatements(String query, RepositoryType repositoryType) {

        return acquireContext()
                .flatMapMany(ctx -> queryServices.queryGraph(query, repositoryType, ctx))
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled()) log.debug("Request to search graph with construct query: {}", query);
                });

    }
}
