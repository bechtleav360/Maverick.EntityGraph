package com.bechtle.cougar.graph.domain.services.handler;

import com.bechtle.cougar.graph.repository.EntityStore;
import com.bechtle.cougar.graph.domain.model.wrapper.AbstractModel;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface Transformer {

    Mono<? extends AbstractModel> handle(EntityStore graph, AbstractModel model, Map<String, String> parameters);
}
