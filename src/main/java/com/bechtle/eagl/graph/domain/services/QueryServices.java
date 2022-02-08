package com.bechtle.eagl.graph.domain.services;

import com.bechtle.eagl.graph.domain.model.extensions.NamespaceAwareStatement;
import com.bechtle.eagl.graph.repository.EntityStore;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Service
public class QueryServices {

    private final EntityStore graph;

    public QueryServices(EntityStore graph) {
        this.graph = graph;
    }


    public Mono<TupleQueryResult> queryValues(String query) {
        return this.graph.queryValues(query);
    }


    public Flux<NamespaceAwareStatement> queryGraph(String query) {
        return this.graph.queryStatements(query);
    }
}
