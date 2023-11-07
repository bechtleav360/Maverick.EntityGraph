package org.av360.maverick.graph.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.rdf.LocalIRI;
import org.av360.maverick.graph.services.IdentifierServices;
import org.eclipse.rdf4j.model.IRI;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class DefaultIdentifierServices implements IdentifierServices {

    public DefaultIdentifierServices() {
        log.debug("dd");
    }

    @Override
    public Mono<String> validate(String identifier, Environment environment) {
        return Mono.just(identifier);
    }


    @Override
    public Mono<IRI> asIRI(String key, String namespace, Environment environment) {
        return this.validate(key, environment).map(checkedKey -> LocalIRI.from(namespace, key));
    }




}
