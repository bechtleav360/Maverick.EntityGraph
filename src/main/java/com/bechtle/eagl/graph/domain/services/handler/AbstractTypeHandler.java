package com.bechtle.eagl.graph.domain.services.handler;

import com.bechtle.eagl.graph.domain.model.wrapper.AbstractModel;
import com.bechtle.eagl.graph.repository.EntityStore;
import reactor.core.publisher.Mono;

import java.util.Map;

public abstract class AbstractTypeHandler {


    public abstract Mono<? extends AbstractModel> handle(EntityStore graph, Mono<? extends AbstractModel> model, Map<String, String> parameters);
}
