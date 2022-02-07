package com.bechtle.eagl.graph.domain.services;

import com.bechtle.eagl.graph.domain.model.extensions.NamespaceAwareStatement;
import com.bechtle.eagl.graph.repository.Graph;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Service
public class AdminServices {

    private final Graph graph;

    public AdminServices(Graph graph) {
        this.graph = graph;
    }


    public Mono<Void> reset() {
        return this.graph.reset();
    }

}
