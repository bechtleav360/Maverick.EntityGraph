package com.bechtle.eagl.graph.repository.rdf4j.repository;

import com.bechtle.eagl.graph.domain.model.vocabulary.Local;
import com.bechtle.eagl.graph.domain.model.vocabulary.SDO;
import com.bechtle.eagl.graph.domain.model.vocabulary.Transactions;
import com.bechtle.eagl.graph.repository.SchemaStore;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.util.Namespaces;
import org.eclipse.rdf4j.repository.Repository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Component
public class SchemaRepository implements SchemaStore {

    private final static Set<Namespace> namespaces;

    static {
        namespaces = new HashSet<>(Namespaces.DEFAULT_RDF4J);
        namespaces.add(Transactions.NS);
        namespaces.add(Local.Entities.NS);
        namespaces.add(Local.Transactions.NS);
        namespaces.add(Local.Versions.NS);
        namespaces.add(SDO.NS);
    }

    private final Repository repository;

    public SchemaRepository(@Qualifier("schema-storage") Repository repository) {
        this.repository = repository;
    }



    @Override
    public Optional<Namespace> getNamespaceFor(String prefix) {
        return namespaces.stream().filter(ns -> ns.getPrefix().equalsIgnoreCase(prefix)).findFirst();
    }

}
