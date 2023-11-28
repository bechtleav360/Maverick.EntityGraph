package org.av360.maverick.graph.services;

import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.identifier.ChecksumIdentifier;
import org.av360.maverick.graph.model.identifier.RandomIdentifier;
import org.av360.maverick.graph.model.rdf.LocalIRI;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;

public interface IdentifierServices {

    String validate(String identifier, Environment environment);


    static IRI buildRandomIRI(String namespace) {
        return new RandomIdentifier(namespace);
    }

    static IRI buildReproducibleIRI(String namespace, Collection<Serializable> parts) {
        return new ChecksumIdentifier(namespace, parts);
    }

    static IRI buildLocalIri(String namespace, String key) {
        return LocalIRI.from(namespace, key);
    }

    default Mono<IRI> asReproducibleLocalIRI(String namespace, Environment environment, Collection<Serializable> parts) {
        return Mono.just(buildReproducibleIRI(namespace, parts));
    }

    default Mono<IRI> asLocalIRI(String key, String namespace, Environment environment) {
        String checkedKey = this.validate(key, environment);
        return Mono.just(buildLocalIri(namespace, checkedKey));
    }

    default Mono<IRI> asRandomLocalIRI(String namespace, Environment environment) {
        return Mono.just(buildRandomIRI(namespace));
    }

    default Mono<IRI> asLocalIRI(String key, Environment environment) {
        return this.asLocalIRI(key, Local.Entities.NAMESPACE, environment);
    }

    default Mono<IRI> asRandomLocalIRI(Namespace ns, Environment environment) {
        return asRandomLocalIRI(ns.getName(), environment);
    }


    default Mono<IRI> asReproducibleLocalIRI(String namespace, Environment environment, Serializable... parts) {
        return asReproducibleLocalIRI(namespace, environment, Arrays.asList(parts));
    }

    default Mono<IRI> asReproducibleLocalIRI(Namespace namespace, Environment environment, Collection<Serializable> parts) {
        return asReproducibleLocalIRI(namespace.getName(), environment, parts);
    }

    default Mono<IRI> asReproducibleLocalIRI(Namespace namespace, Environment environment, Serializable... parts) {
        return asReproducibleLocalIRI(namespace.getName(), environment, Arrays.asList(parts));
    }








}
