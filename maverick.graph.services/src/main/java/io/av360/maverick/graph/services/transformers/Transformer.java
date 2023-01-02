package io.av360.maverick.graph.services.transformers;

import io.av360.maverick.graph.services.EntityServices;
import io.av360.maverick.graph.services.QueryServices;
import io.av360.maverick.graph.store.rdf.models.TripleModel;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface Transformer {

    Mono<? extends TripleModel> handle(TripleModel model, Map<String, String> parameters, Authentication authentication);

    default void registerEntityService(EntityServices entityServicesImpl) {
    }

    default void registerQueryService(QueryServices queryServices) {
    }
}
