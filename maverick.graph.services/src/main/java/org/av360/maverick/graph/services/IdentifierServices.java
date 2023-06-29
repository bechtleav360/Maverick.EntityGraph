package org.av360.maverick.graph.services;

import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.identifier.ChecksumIdentifier;
import org.av360.maverick.graph.model.identifier.LocalIdentifier;
import org.av360.maverick.graph.model.identifier.RandomIdentifier;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import reactor.core.publisher.Mono;

import java.io.Serializable;

public interface IdentifierServices {

    Mono<String> validate(String identifier, Environment environment);


    Mono<IRI> asIRI(String key, String namespace, Environment environment);

    default Mono<IRI> asIRI(String key, Environment environment) {
        return this.asIRI(key, Local.Entities.NAMESPACE, environment);
    }

    default Mono<IRI> asRandomIRI(String namespace, Environment environment) {
        return Mono.just(createRandomIdentifier(namespace));
    }

    default Mono<IRI> asReproducibleIRI(String namespace, Environment environment, Serializable... parts) {
        return Mono.just(createReproducibleIdentifier(namespace, parts));
    }

    default Mono<IRI> asReproducibleIRI(Namespace namespace, Environment environment, Serializable... parts) {
        return asReproducibleIRI(namespace.getName(), environment, parts);
    }

    static LocalIdentifier createRandomIdentifier(String namespace) {
        return new RandomIdentifier(namespace);
    }

    static LocalIdentifier createReproducibleIdentifier(String namespace, Serializable... parts) {
        return new ChecksumIdentifier(namespace, parts);
    }

    static LocalIdentifier createReproducibleIdentifier(Namespace namespace, Serializable ... parts) {
        return createReproducibleIdentifier(namespace.getName(), parts);
    }


    default Mono<IRI> asRandomIRI(Namespace ns, Environment environment) {
        return asRandomIRI(ns.getName(), environment);
    }
}
