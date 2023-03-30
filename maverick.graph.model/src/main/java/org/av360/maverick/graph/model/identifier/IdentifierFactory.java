package org.av360.maverick.graph.model.identifier;

import org.eclipse.rdf4j.model.Namespace;

import java.io.Serializable;

public interface IdentifierFactory {

    LocalIdentifier createRandomIdentifier(String namespace);

    default LocalIdentifier createRandomIdentifier(Namespace namespace) {
        return createRandomIdentifier(namespace.getName());
    }

    LocalIdentifier createReproducibleIdentifier(String namespace, Serializable ... parts);

    default LocalIdentifier createReproducibleIdentifier(Namespace namespace, Serializable ... parts) {
        return createReproducibleIdentifier(namespace.getName(), parts);
    }

}
