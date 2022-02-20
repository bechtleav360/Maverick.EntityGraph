package com.bechtle.eagl.graph.domain.model.enums;

import com.bechtle.eagl.graph.domain.model.vocabulary.Local;
import com.bechtle.eagl.graph.domain.model.vocabulary.Transactions;
import com.bechtle.eagl.graph.domain.model.wrapper.Transaction;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleIRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * The different activities which can be performed on an entity (and are as such store in the provenance information)
 */
public enum Activity {

    INSERTED("inserted"),
    REMOVED("removed"),
    UPDATED("updated");


    private final String stringValue;

    Activity(String stringValue) {

        this.stringValue = stringValue;
    }


    @Override
    public String toString() {
        return this.stringValue;
    }

    public IRI toIRI() {
        return SimpleValueFactory.getInstance().createIRI(Transactions.NAMESPACE, this.stringValue);
    }
}
