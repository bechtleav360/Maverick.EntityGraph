package com.bechtle.eagl.graph.connector.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Vocabularies;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SD;

import javax.print.attribute.standard.MediaSize;

public class Transactions {

        private static ValueFactory vf;
        static {
                vf = SimpleValueFactory.getInstance();
        }


        public static final String NAMESPACE = "http://www.av360.io/schema/transactions#";
        public static final String PREFIX = "tract";
        // FIXME: make configurable
        public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);
        public static final IRI TRANSACTION = PROV.ACTIVITY;
        public static final IRI MODIFIED_RESOURCE = PROV.USED;

        public static final IRI TRANSACTION_TIME = PROV.AT_TIME;

        public Transactions() {
        }
}
