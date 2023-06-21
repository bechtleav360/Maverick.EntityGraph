package org.av360.maverick.graph.store;

import org.av360.maverick.graph.store.behaviours.Maintainable;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import reactor.core.publisher.Flux;

public interface SchemaStore extends Maintainable {

    ValueFactory getValueFactory();

    Flux<Namespace> listNamespaces();

}
