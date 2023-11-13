package org.av360.maverick.graph.services;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import reactor.core.publisher.Mono;

public interface SchemaServices {


    /**
     * Since we cannot encode fully qualified names (URIs and URNs) in the URls, we use prefixed names separated by a dot.
     * The property "https://schema.org/title" will be "sdo.title"
     *
     * @param prefixedName the identifier, consisting of prefix and local name, separated by a dot
     * @return
     */
    Mono<IRI> resolvePrefixedName(String prefixedName);

    /**
     * Converts a local name (e.g. the entity key) into a qualified IRI using the configured local (but resolvable) namespace.
     *
     * @param name
     * @return
     * @deprecated use identifierservice instead
     */
    Mono<IRI> resolveLocalName(String name);

    Mono<Namespace> getNamespaceFor(String prefix);

    boolean isIndividualType(IRI iri);

    boolean isClassifierType(IRI iri);

    boolean isCharacteristicProperty(IRI iri);
}
