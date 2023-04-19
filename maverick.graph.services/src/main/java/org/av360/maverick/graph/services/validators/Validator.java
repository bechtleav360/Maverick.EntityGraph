package org.av360.maverick.graph.services.validators;

import org.av360.maverick.graph.services.EntityServices;
import org.av360.maverick.graph.store.rdf.fragments.TripleModel;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface Validator {

    Mono<? extends TripleModel> handle(EntityServices entityServicesImpl, TripleModel model, Map<String, String> parameters);
}
