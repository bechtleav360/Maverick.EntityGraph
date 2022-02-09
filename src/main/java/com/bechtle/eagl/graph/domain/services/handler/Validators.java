package com.bechtle.eagl.graph.domain.services.handler;

import com.bechtle.eagl.graph.domain.model.wrapper.AbstractModelWrapper;
import com.bechtle.eagl.graph.domain.services.handler.validators.TypeIsRequired;
import com.bechtle.eagl.graph.repository.EntityStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;

@Component
public class Validators {
    private final Set<AbstractTypeHandler> registeredHandlers;

    public Validators() {
        registeredHandlers = Set.of(
                new TypeIsRequired()
        );
    }

    public Mono<? extends AbstractModelWrapper> delegate(Mono<? extends AbstractModelWrapper> triples, EntityStore graph, Map<String, String> parameters) {
        for(AbstractTypeHandler handler : registeredHandlers) {
            triples = handler.handle(graph, triples, parameters);
        }
        return triples;
    }
}
