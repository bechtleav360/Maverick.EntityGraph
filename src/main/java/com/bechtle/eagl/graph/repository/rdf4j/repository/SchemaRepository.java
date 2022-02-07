package com.bechtle.eagl.graph.repository.rdf4j.repository;

import com.bechtle.eagl.graph.domain.model.vocabulary.Transactions;
import com.bechtle.eagl.graph.repository.Schema;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.eclipse.rdf4j.repository.Repository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Component
public class SchemaRepository implements Schema {

    private static Set<Namespace> namespaces;

    static {
        namespaces = Set.of(
                PROV.NS,
                RDF.NS,
                RDFS.NS,
                Transactions.NS,
                DC.NS,
                DCTERMS.NS,
                FOAF.NS,
                ORG.NS,
                SKOS.NS,
                XSD.NS
        );
    }

    private final Repository repository;

    public SchemaRepository(Repository repository) {
        this.repository = repository;
    }



    @Override
    public Optional<Namespace> getNamespaceFor(String prefix) {
        return namespaces.stream().filter(ns -> ns.getPrefix().equalsIgnoreCase(prefix)).findFirst();
    }

}
