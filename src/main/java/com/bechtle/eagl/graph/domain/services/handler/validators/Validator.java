package com.bechtle.eagl.graph.domain.services.handler.validators;

import com.bechtle.eagl.graph.domain.model.wrapper.AbstractModel;
import com.bechtle.eagl.graph.repository.EntityStore;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface Validator {

    Mono<? extends AbstractModel> handle(EntityStore graph, AbstractModel model, Map<String, String> parameters);
}
