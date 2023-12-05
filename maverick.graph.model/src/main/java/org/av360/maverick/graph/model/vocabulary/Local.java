package org.av360.maverick.graph.model.vocabulary;

import org.av360.maverick.graph.model.rdf.LocalIRI;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.util.Values;

/**
 * Constructed from the local domain. Constructed through configuration
 * <p>
 * The prefixes here are also used to identify the responsible repository.
 */
public class Local {

    public static final String NAMESPACE = "http://w3id.org/avs/emav#";
    public static final String PREFIX = "meg";
    public static String URN_PREFIX = "urn:pwid:meg:";
    public static final IRI ORIGINAL_IDENTIFIER = LocalIRI.from("urn:int:", "srcid");
    public static String Files;
    public static class Classifier {
        public static String NAME = URN_PREFIX+"c:";
        public static String PREFIX = "cls";
        public static Namespace NAMESPACE = Values.namespace(PREFIX, NAME);
    }

    public static class Schema {
        public static String NAME = URN_PREFIX+"v:";
        public static String PREFIX = "voc";
        public static Namespace NAMESPACE = Values.namespace(PREFIX, NAME);
    }


    public static class Entities {

        public static String NAME = URN_PREFIX+"e:";

        public static String PREFIX = "meg";
        public static Namespace NS = Values.namespace(PREFIX, NAME);

        public static final IRI TYPE_INDIVIDUAL = Values.iri(NAME, "Individual");
        public static final IRI TYPE_CLASSIFIER = Values.iri(NAME, "Classifier");
        public static final IRI TYPE_EMBEDDED = Values.iri(NAME, "Embedded");

        public static final IRI HASH = Values.iri(NAME, "hash");
    }

    public static class Objects {

        public static String NAME = URN_PREFIX+"o:";

        public static String PREFIX = "file";
        public static Namespace NS = Values.namespace(PREFIX, NAME);

    }


    public static class Transactions {
        public static String NAME = URN_PREFIX+"t:";
        public static String PREFIX = "trx";
        public static Namespace NAMESPACE = Values.namespace(PREFIX, NAME);
    }

    public static class Versions {
        public static String NAME = URN_PREFIX+"v:";
        public static String PREFIX = "vrs";
        public static Namespace NAMESPACE = Values.namespace(PREFIX, NAME);
    }

    public static class Applications {
        public static String NAME = URN_PREFIX+"a:";
        public static String PREFIX = "app";
        public static Namespace NAMESPACE = Values.namespace(PREFIX, NAME);
    }

}
