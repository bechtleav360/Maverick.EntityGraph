package org.av360.maverick.graph.model.enums;

import org.av360.maverick.graph.model.vocabulary.meg.Transactions;
import org.eclipse.rdf4j.model.IRI;
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
