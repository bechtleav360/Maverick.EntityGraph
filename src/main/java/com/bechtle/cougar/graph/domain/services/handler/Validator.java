package com.bechtle.cougar.graph.domain.services.handler;

import com.bechtle.cougar.graph.domain.services.EntityServices;
import com.bechtle.cougar.graph.repository.EntityStore;
import com.bechtle.cougar.graph.domain.model.wrapper.AbstractModel;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface Validator {

    Mono<? extends AbstractModel> handle(EntityServices entityServices, AbstractModel model, Map<String, String> parameters);
}
