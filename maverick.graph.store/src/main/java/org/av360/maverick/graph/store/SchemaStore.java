package org.av360.maverick.graph.store;

import org.av360.maverick.graph.model.enums.RepositoryType;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import reactor.core.publisher.Flux;

import java.util.Optional;

public interface SchemaStore extends FragmentsStore {


    ValueFactory getValueFactory();

    Flux<Namespace> listNamespaces();

    Optional<Namespace> getNamespaceForPrefix(String key);

    @Override
    default RepositoryType getRepositoryType() {
        return RepositoryType.SCHEMA;
    }
}
