package com.bechtle.eagl.graph.domain.model.vocabulary;

import com.bechtle.eagl.graph.domain.model.extensions.EntityIRI;
import com.bechtle.eagl.graph.domain.model.extensions.EntityNamespace;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.vocabulary.PROV;

public class Transactions {

        public static final String NAMESPACE = "http://bechtleav360.github.io/vocab/transactions#";
        public static final String PREFIX = "trx";
        // FIXME: make configurable
        public static final Namespace NS = EntityNamespace.of(PREFIX, NAMESPACE);

        // subClassOf PROV.ACTIVITY
        public static final IRI TRANSACTION = EntityIRI.from(NAMESPACE, "Transaction");


        // subClassOf PROV.USED
        public static final IRI CREATED = EntityIRI.from(NAMESPACE, "created");

        // subClassOf PROV.USED
        public static final IRI DELETED = EntityIRI.from(NAMESPACE, "deleted");

        // subClassOf PROV.USED
        public static final IRI UPDATED = EntityIRI.from(NAMESPACE, "updated");

        public static final IRI AT = PROV.AT_TIME;

        public static final IRI BY = PROV.WAS_ATTRIBUTED_TO;

        public static final IRI VERSION = PROV.QUALIFIED_REVISION;


        public Transactions() {
        }
}
