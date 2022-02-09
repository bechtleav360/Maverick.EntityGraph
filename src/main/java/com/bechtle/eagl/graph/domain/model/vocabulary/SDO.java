package com.bechtle.eagl.graph.domain.model.vocabulary;

import com.bechtle.eagl.graph.domain.model.extensions.EntityIRI;
import com.bechtle.eagl.graph.domain.model.extensions.EntityNamespace;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

public class SDO {



        public static final String NAMESPACE = "http://schema.org/";
        public static final String PREFIX = "sdo";
        public static final Namespace NS = EntityNamespace.of(PREFIX, NAMESPACE);

        public static final IRI DEFINED_TERM = EntityIRI.from(NAMESPACE, "DefinedTerm");

        public SDO() {
        }
}
