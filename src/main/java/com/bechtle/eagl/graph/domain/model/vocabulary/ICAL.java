package com.bechtle.eagl.graph.domain.model.vocabulary;

import com.bechtle.eagl.graph.domain.model.extensions.EntityIRI;
import com.bechtle.eagl.graph.domain.model.extensions.EntityNamespace;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.vocabulary.PROV;

public class ICAL {



        public static final String NAMESPACE = "http://www.w3.org/2002/12/cal/ical#";
        public static final String PREFIX = "ical";
        public static final Namespace NS = EntityNamespace.of(PREFIX, NAMESPACE);
        public static final org.eclipse.rdf4j.model.IRI VEVENT = EntityIRI.from(NAMESPACE, "VEVENT");

        public ICAL() {
        }
}
