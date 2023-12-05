package org.av360.maverick.graph.model.vocabulary;

import org.av360.maverick.graph.model.rdf.EntityNamespace;
import org.av360.maverick.graph.model.rdf.LocalIRI;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.PROV;

public class Transactions {
    public static final Literal RUNNING = SimpleValueFactory.getInstance().createLiteral("running");
    public static final Literal SUCCESS = SimpleValueFactory.getInstance().createLiteral("success");
    public static final Literal FAILURE = SimpleValueFactory.getInstance().createLiteral("failure");


    public static final String NAMESPACE = "https://w3id.org/av360/megt#";
    public static final String PREFIX = "megt";
    // FIXME: make configurable
    public static final Namespace NS = EntityNamespace.of(PREFIX, NAMESPACE);

    // subClassOf PROV.ACTIVITY
    public static final IRI TRANSACTION = LocalIRI.from(NAMESPACE, "Transaction");


    public static final IRI AT = PROV.AT_TIME;

    public static final IRI BY = PROV.WAS_ATTRIBUTED_TO;

    public static final IRI STATUS = LocalIRI.from(NAMESPACE, "status");

    public static final IRI VERSION = PROV.QUALIFIED_REVISION;


    public static final IRI GRAPH_DELETED = LocalIRI.from(Local.Transactions.NAME, "RemovedStatements");
    public static final IRI GRAPH_CREATED = LocalIRI.from(Local.Transactions.NAME, "InsertedStatements");
    public static final IRI GRAPH_UPDATED = LocalIRI.from(Local.Transactions.NAME, "UpdatedStatements");
    public static final IRI GRAPH_AFFECTED = LocalIRI.from(Local.Transactions.NAME, "AffectedResource");
    public static final IRI GRAPH_PROVENANCE = LocalIRI.from(Local.Transactions.NAME, "ProvenanceStatements");

    public static final IRI FAILURE_REASON = LocalIRI.from(NAMESPACE, "reason");


    public Transactions() {
    }
}
