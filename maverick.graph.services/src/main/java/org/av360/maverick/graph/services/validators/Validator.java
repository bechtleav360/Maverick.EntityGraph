package org.av360.maverick.graph.services.validators;

import org.av360.maverick.graph.services.EntityServices;
import org.eclipse.rdf4j.model.Model;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface Validator {

    Mono<? extends Model> handle(EntityServices entityServicesImpl, Model model, Map<String, String> parameters);
}
