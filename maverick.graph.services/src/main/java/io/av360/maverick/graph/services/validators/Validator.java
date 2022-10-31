package io.av360.maverick.graph.services.validators;

import io.av360.maverick.graph.services.EntityServices;
import io.av360.maverick.graph.store.rdf.models.AbstractModel;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface Validator {

    Mono<? extends AbstractModel> handle(EntityServices entityServicesImpl, AbstractModel model, Map<String, String> parameters);
}
