package org.av360.maverick.graph.model.vocabulary;

import org.av360.maverick.graph.model.rdf.EntityNamespace;
import org.av360.maverick.graph.model.rdf.LocalIRI;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * Constructed from the local domain. Constructed through configuration
 * <p>
 * The prefixes here are also used to identify the responsible repository.
 */
public class Local {

    public static final String NAMESPACE = "http://w3id.org/av360/emav#";
    public static final String PREFIX = "meg";
    public static String URN_PREFIX = "urn:pwid:meg:";
    public static final IRI ORIGINAL_IDENTIFIER = LocalIRI.from("urn:int:", "srcid");



    public static class Entities {

        public static String NAMESPACE = URN_PREFIX+"e:";

        public static String PREFIX = "ent";
        public static Namespace NS = EntityNamespace.of(PREFIX, NAMESPACE);

        public static final IRI INDIVIDUAL = LocalIRI.from(NAMESPACE, "Individual");
        public static final IRI CLASSIFIER = LocalIRI.from(NAMESPACE, "Classifier");
        public static final IRI EMBEDDED = LocalIRI.from(NAMESPACE, "Embedded");
    }


    public static class Transactions {
        public static String NAMESPACE = URN_PREFIX+"t:";
        public static String PREFIX = "trx";
        public static Namespace NS = EntityNamespace.of(PREFIX, NAMESPACE);
    }

    public static class Versions {
        public static String NAMESPACE = URN_PREFIX+"v:";
        public static String PREFIX = "vrs";
        public static Namespace NS = EntityNamespace.of(PREFIX, NAMESPACE);
    }

    public static class Applications {
        public static String NAMESPACE = URN_PREFIX+"a:";
        public static String PREFIX = "app";
        public static Namespace NS = EntityNamespace.of(PREFIX, NAMESPACE);
    }

}
