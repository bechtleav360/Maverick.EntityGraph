package org.av360.maverick.graph.services.impl;

import org.av360.maverick.graph.model.rdf.LocalIRI;
import org.av360.maverick.graph.services.IdentifierServices;
import org.eclipse.rdf4j.model.IRI;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class IdentifierServicesImpl implements IdentifierServices {
    @Override
    public Mono<String> validate(String identifier) {
        return Mono.just(identifier);
    }

    @Override
    public Mono<IRI> asIRI(String key) {
       return this.validate(key).map(checkedKey -> LocalIRI.withDefaultNamespace(key));
    }
}
