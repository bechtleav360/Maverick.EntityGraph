package com.bechtle.cougar.graph.domain.services;

import com.bechtle.cougar.graph.domain.services.handler.DelegatingTransformer;
import com.bechtle.cougar.graph.repository.EntityStore;
import com.bechtle.cougar.graph.domain.model.extensions.NamespaceAwareStatement;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Service
@Slf4j(topic = "cougar.graph.service.query")
public class QueryServices {

    private final EntityStore entityStore;

    public QueryServices(EntityStore graph) {
        this.entityStore = graph;
    }


    public Flux<BindingSet> queryValues(String query, Authentication authentication) {
        return this.entityStore.query(query, authentication)
                .doOnSubscribe(subscription -> {
                    log.trace("Running query in entity store.");
                });
    }

    public Flux<BindingSet> queryValues(SelectQuery query, Authentication authentication) {
        return this.queryValues(query.getQueryString(), authentication);
    }

    public Flux<NamespaceAwareStatement> queryGraph(String query, Authentication authentication) {
        return this.entityStore.construct(query, authentication)
                .doOnSubscribe(subscription -> {
                    log.trace("Running query in entity store.");
                });

    }


    @Autowired
    public void linkTransformers(DelegatingTransformer transformers) {
        transformers.registerQueryService(this);
    }


}
