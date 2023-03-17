package io.av360.maverick.graph.model.vocabulary;

import io.av360.maverick.graph.model.rdf.EntityNamespace;
import io.av360.maverick.graph.model.rdf.LocalIRI;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * Constructed from the local domain. Constructed through configuration
 * <p>
 * The prefixes here are also used to identify the responsible repository.
 */
public class Local {

    public static final String NAMESPACE = "http://w3id.org/av360/emav#";
    public static final IRI ORIGINAL_IDENTIFIER = LocalIRI.from(NAMESPACE, "sourceId");


    public static class Entities {
        public static String NAMESPACE = "urn:pwid:eg:lef:";
        public static String PREFIX = "lef";
        public static Namespace NS = EntityNamespace.of(PREFIX, NAMESPACE);

        public static IRI TYPE = LocalIRI.from(NAMESPACE, "Entity");


    }


    public static class Transactions {
        public static String NAMESPACE = "urn:pwid:eg:trx:";
        public static String PREFIX = "trx";
        public static Namespace NS = EntityNamespace.of(PREFIX, NAMESPACE);
    }

    public static class Versions {
        public static String NAMESPACE = "urn:pwid:eg:vs:";
        public static String PREFIX = "vs";
        public static Namespace NS = EntityNamespace.of(PREFIX, NAMESPACE);
    }

    public static class Subscriptions {
        public static String NAMESPACE = "urn:pwid:app :";
        public static String PREFIX = "app";
        public static Namespace NS = EntityNamespace.of(PREFIX, NAMESPACE);
    }

}
