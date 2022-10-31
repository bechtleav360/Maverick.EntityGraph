package io.av360.maverick.graph.store;

import org.eclipse.rdf4j.model.Namespace;

import java.util.Optional;

public interface SchemaStore {
    Optional<Namespace> getNamespaceFor(String prefix);
}
