package com.bechtle.cougar.graph.domain.services;

import com.bechtle.cougar.graph.repository.EntityStore;
import com.bechtle.cougar.graph.domain.model.extensions.NamespaceAwareStatement;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.mapdb.Bind;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Service
@Slf4j(topic = "cougar.graph.service.query")
public class QueryServices {

    private final EntityStore graph;

    public QueryServices(EntityStore graph) {
        this.graph = graph;
    }


    public Flux<BindingSet> queryValues(String query) {
        return this.graph.query(query)
                .doOnSubscribe(subscription -> {
                    log.trace("Running query in entity store.");
                });
    }


    public Flux<NamespaceAwareStatement> queryGraph(String query) {
        return this.graph.constructQuery(query);
    }
}
