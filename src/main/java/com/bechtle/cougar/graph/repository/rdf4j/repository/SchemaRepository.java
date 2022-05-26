package com.bechtle.cougar.graph.repository.rdf4j.repository;

import com.bechtle.cougar.graph.repository.rdf4j.config.RepositoryConfiguration;
import com.bechtle.cougar.graph.domain.model.vocabulary.Local;
import com.bechtle.cougar.graph.domain.model.vocabulary.SDO;
import com.bechtle.cougar.graph.domain.model.vocabulary.Transactions;
import com.bechtle.cougar.graph.repository.SchemaStore;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.util.Namespaces;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;
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
        super(RepositoryConfiguration.RepositoryType.SCHEMA);
    }


    @Override
    public Optional<Namespace> getNamespaceFor(String prefix) {
        return namespaces.stream().filter(ns -> ns.getPrefix().equalsIgnoreCase(prefix)).findFirst();
    }

}
