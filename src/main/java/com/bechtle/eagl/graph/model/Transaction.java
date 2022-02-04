package com.bechtle.eagl.graph.model;

import com.bechtle.eagl.graph.connector.rdf4j.model.GeneratedIdentifier;
import com.bechtle.eagl.graph.connector.rdf4j.model.vocabulary.Transactions;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.util.*;
import java.util.stream.Stream;

/**
 * Convenience methods to build a tranction. Wraps a model
 */
public class Transaction implements NamespaceAware {
    private final ModelBuilder model;

    public Transaction() {
        this.model = new ModelBuilder();
        this.model.setNamespace(PROV.NS)
                .setNamespace(Transactions.NS)
                .subject(new GeneratedIdentifier(Transactions.NAMESPACE))
                .add(RDF.TYPE, Transactions.TRANSACTION)
                .add(Transactions.TRANSACTION_TIME, SimpleValueFactory.getInstance().createLiteral(new Date()));
    }



    public void addModifiedResource(Resource resource) {
        this.model.add(Transactions.MODIFIED_RESOURCE, resource);
    }

    @Override
    public Set<Namespace> getNamespaces() {
        return this.getModel().getNamespaces();
    }

    public Model getModel() {
        return model.build();
    }

    public Stream<NamespaceAwareStatement> stream() {
        return this.getModel().stream().map(sts -> NamespaceAwareStatement.wrap(sts, getNamespaces()));
    }
}
