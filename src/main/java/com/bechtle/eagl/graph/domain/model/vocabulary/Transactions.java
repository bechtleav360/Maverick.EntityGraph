package com.bechtle.eagl.graph.domain.model.vocabulary;

import com.bechtle.eagl.graph.domain.model.extensions.EntityNamespace;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.vocabulary.PROV;

public class Transactions {

        public static final String NAMESPACE = "http://www.av360.io/schema/transactions#";
        public static final String PREFIX = "tract";
        // FIXME: make configurable
        public static final Namespace NS = EntityNamespace.of(PREFIX, NAMESPACE);
        public static final IRI TRANSACTION = PROV.ACTIVITY;
        public static final IRI MODIFIED_RESOURCE = PROV.USED;

        public static final IRI TRANSACTION_TIME = PROV.AT_TIME;

        public Transactions() {
        }
}
