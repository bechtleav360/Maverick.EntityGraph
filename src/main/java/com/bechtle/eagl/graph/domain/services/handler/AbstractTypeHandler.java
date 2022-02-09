package com.bechtle.eagl.graph.domain.services.handler;

import com.bechtle.eagl.graph.domain.model.wrapper.AbstractModelWrapper;
import com.bechtle.eagl.graph.repository.EntityStore;
import org.eclipse.rdf4j.model.Resource;
import reactor.core.publisher.Mono;

import java.util.Map;

public abstract class AbstractTypeHandler {

    public abstract boolean handlesType(Resource object);

    public abstract Mono<? extends AbstractModelWrapper> handle(EntityStore graph, Mono<? extends AbstractModelWrapper> model, Map<String, String> parameters);
}
