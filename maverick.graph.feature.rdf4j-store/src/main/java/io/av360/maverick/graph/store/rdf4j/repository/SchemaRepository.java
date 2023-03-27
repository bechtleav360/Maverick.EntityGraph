package io.av360.maverick.graph.store.rdf4j.repository;

import io.av360.maverick.graph.model.vocabulary.Local;
import io.av360.maverick.graph.model.vocabulary.SDO;
import io.av360.maverick.graph.model.vocabulary.Transactions;
import io.av360.maverick.graph.store.RepositoryType;
import io.av360.maverick.graph.store.SchemaStore;
import io.av360.maverick.graph.store.rdf4j.repository.util.AbstractRepository;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Namespaces;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashSet;
import java.util.Set;

@Component
public class SchemaRepository extends AbstractRepository implements SchemaStore {

    private final static Set<Namespace> namespaces;

    static {
        namespaces = new HashSet<>(Namespaces.DEFAULT_RDF4J);
        namespaces.add(Transactions.NS);
        namespaces.add(Local.Entities.NS);
        namespaces.add(Local.Transactions.NS);
        namespaces.add(Local.Versions.NS);
        namespaces.add(SDO.NS);
    }


    public SchemaRepository() {
        super(RepositoryType.SCHEMA);
    }



    @Override
    public ValueFactory getValueFactory() {
        return SimpleValueFactory.getInstance();
    }

    @Override
    public Flux<Namespace> listNamespaces() {
        return Flux.fromIterable(namespaces);
    }

}
