package com.bechtle.eagl.graph.domain.model.vocabulary;

import com.bechtle.eagl.graph.domain.model.extensions.EntityNamespace;
import org.eclipse.rdf4j.model.Namespace;

public class EAGL {



        public static final String NAMESPACE = "http://av360.io/schema/eagl/";
        public static final String PREFIX = "eagl";
        public static final Namespace NS = EntityNamespace.of(PREFIX, NAMESPACE);

        public EAGL() {
        }
}
