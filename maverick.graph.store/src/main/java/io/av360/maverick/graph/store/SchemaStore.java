package io.av360.maverick.graph.store;

import org.eclipse.rdf4j.model.ValueFactory;

public interface SchemaStore {
    String getNamespaceFor(String prefix);

    ValueFactory getValueFactory();
}
