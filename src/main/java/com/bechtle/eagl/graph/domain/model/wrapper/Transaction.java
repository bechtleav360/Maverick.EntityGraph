package com.bechtle.eagl.graph.domain.model.wrapper;

import com.bechtle.eagl.graph.domain.model.extensions.GeneratedIdentifier;
import com.bechtle.eagl.graph.domain.model.vocabulary.Local;
import com.bechtle.eagl.graph.domain.model.vocabulary.Transactions;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Convenience methods to build a tranction. Wraps a model
 */
public class Transaction extends AbstractModelWrapper  {
    private final GeneratedIdentifier transactionIdentifier;

    public Transaction() {
        super();

        transactionIdentifier = new GeneratedIdentifier(Local.Transactions.NAMESPACE);

        super.getBuilder()
                .setNamespace(PROV.NS)
                .setNamespace(Local.Transactions.NS)
                .subject(transactionIdentifier)
                .add(RDF.TYPE, Transactions.TRANSACTION)
                .add(Transactions.TRANSACTION_TIME, SimpleValueFactory.getInstance().createLiteral(new Date()));
    }



    public void addModifiedResource(Resource resource) {
        this.getBuilder().add(Transactions.MODIFIED_RESOURCE, resource);
    }

    public List<Value> listModifiedResources() {
        return super.streamValues(transactionIdentifier, Transactions.MODIFIED_RESOURCE).collect(Collectors.toList());
    }
}
