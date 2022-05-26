package com.bechtle.cougar.graph.domain.services;

import com.bechtle.cougar.graph.repository.EntityStore;
import com.bechtle.cougar.graph.domain.model.extensions.NamespaceAwareStatement;
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
        return this.graph.select(query);
    }


    public Flux<NamespaceAwareStatement> queryGraph(String query) {
        return this.graph.constructQuery(query);
    }
}
