package io.av360.maverick.graph.services.services.handler;

import io.av360.maverick.graph.services.services.EntityServices;
import io.av360.maverick.graph.services.services.QueryServices;
import io.av360.maverick.graph.store.rdf.models.AbstractModel;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface Transformer {

    Mono<? extends AbstractModel> handle(AbstractModel model, Map<String, String> parameters, Authentication authentication);

    default void registerEntityService(EntityServices entityServices) {}

    default void registerQueryService(QueryServices queryServices) {}
}
