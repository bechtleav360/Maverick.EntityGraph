package com.bechtle.eagl.graph.services;

import com.bechtle.eagl.graph.model.NamespaceAwareStatement;
import com.bechtle.eagl.graph.repository.Graph;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Service
public class QueryServices {

    private final Graph graph;

    public QueryServices(Graph graph) {
        this.graph = graph;
    }


    public Mono<TupleQueryResult> queryValues(String query) {
        return this.graph.queryValues(query);
    }


    public Flux<NamespaceAwareStatement> queryGraph(String query) {
        return this.graph.queryStatements(query);
    }
}
