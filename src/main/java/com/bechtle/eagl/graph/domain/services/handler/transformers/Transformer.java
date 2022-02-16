package com.bechtle.eagl.graph.domain.services.handler.transformers;

import com.bechtle.eagl.graph.domain.model.wrapper.AbstractModel;
import com.bechtle.eagl.graph.repository.EntityStore;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.BiConsumer;

public interface Transformer {

    Mono<? extends AbstractModel> handle(EntityStore graph, AbstractModel model, Map<String, String> parameters);
}
