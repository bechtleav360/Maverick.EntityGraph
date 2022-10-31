package io.av360.maverick.graph.services.services.handler;

import io.av360.maverick.graph.services.services.EntityServices;
import io.av360.maverick.graph.store.rdf.models.AbstractModel;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface Validator {

    Mono<? extends AbstractModel> handle(EntityServices entityServices, AbstractModel model, Map<String, String> parameters);
}
