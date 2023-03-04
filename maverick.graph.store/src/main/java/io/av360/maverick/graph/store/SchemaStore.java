package io.av360.maverick.graph.store;

import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import reactor.core.publisher.Flux;

public interface SchemaStore {

    ValueFactory getValueFactory();

    Flux<Namespace> listNamespaces();
}
