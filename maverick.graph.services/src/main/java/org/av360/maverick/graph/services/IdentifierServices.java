package org.av360.maverick.graph.services;

import org.eclipse.rdf4j.model.IRI;
import reactor.core.publisher.Mono;

public interface IdentifierServices {

    Mono<String> validate(String identifier);

    Mono<IRI> asIRI(String key);
}
