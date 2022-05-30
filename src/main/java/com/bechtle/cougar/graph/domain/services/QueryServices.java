package com.bechtle.cougar.graph.domain.services;

import com.bechtle.cougar.graph.repository.EntityStore;
import com.bechtle.cougar.graph.domain.model.extensions.NamespaceAwareStatement;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.query.BindingSet;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;


@Service
@Slf4j(topic = "cougar.graph.service.query")
public class QueryServices {

    private final EntityStore entityStore;

    public QueryServices(EntityStore graph) {
        this.entityStore = graph;
    }


    public Flux<BindingSet> queryValues(String query) {
        return ReactiveSecurityContextHolder.getContext().map(SecurityContext::getAuthentication)
                .flatMapMany(authentication ->
                        this.entityStore.query(query, authentication)
                                .doOnSubscribe(subscription -> {
                                    log.trace("Running query in entity store.");
                                })
                );
    }


    public Flux<NamespaceAwareStatement> queryGraph(String query) {
        return ReactiveSecurityContextHolder.getContext().map(SecurityContext::getAuthentication)
                .flatMapMany(authentication ->
                        this.entityStore.construct(query, authentication)
                                .doOnSubscribe(subscription -> {
                                    log.trace("Running query in entity store.");
                                })
                );

    }
}
