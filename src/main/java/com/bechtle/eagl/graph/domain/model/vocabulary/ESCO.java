package com.bechtle.eagl.graph.domain.model.vocabulary;

import com.bechtle.eagl.graph.domain.model.extensions.EntityIRI;
import com.bechtle.eagl.graph.domain.model.extensions.EntityNamespace;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

public class ESCO {



        public static final String NAMESPACE = "http://data.europa.eu/esco/model#";
        public static final String PREFIX = "esco";
        public static final Namespace NS = EntityNamespace.of(PREFIX, NAMESPACE);

        public ESCO() {
        }
}
