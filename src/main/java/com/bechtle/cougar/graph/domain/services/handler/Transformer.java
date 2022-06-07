package com.bechtle.cougar.graph.domain.services.handler;

import com.bechtle.cougar.graph.domain.services.EntityServices;
import com.bechtle.cougar.graph.domain.services.QueryServices;
import com.bechtle.cougar.graph.repository.EntityStore;
import com.bechtle.cougar.graph.domain.model.wrapper.AbstractModel;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface Transformer {

    Mono<? extends AbstractModel> handle(AbstractModel model, Map<String, String> parameters, Authentication authentication);

    default void registerEntityService(EntityServices entityServices) {}

    default void registerQueryService(QueryServices queryServices) {}
}
