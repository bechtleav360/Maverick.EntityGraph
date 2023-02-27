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

    public static final String NAMESPACE = "http://bechtleav360.github.io/vocab/graph#";
    public static final IRI ORIGINAL_IDENTIFIER = LocalIRI.from(NAMESPACE, "sourceId");


    public static class Entities {
        public static String NAMESPACE = "http://entitygraph.azurewebsites.net/api/entities/";
        public static String PREFIX = "entity";
        public static Namespace NS = EntityNamespace.of(PREFIX, NAMESPACE);

        public static IRI TYPE = LocalIRI.from(NAMESPACE, "Entity");


    }


    public static class Transactions {
        public static String NAMESPACE = "http://entitygraph.azurewebsites.net/api/transactions/";
        public static String PREFIX = "transaction";
        public static Namespace NS = EntityNamespace.of(PREFIX, NAMESPACE);
    }

    public static class Versions {
        public static String NAMESPACE = "http://graphs.azurewebsites.net/api/versions/";
        public static String PREFIX = "version";
        public static Namespace NS = EntityNamespace.of(PREFIX, NAMESPACE);
    }

    public static class Subscriptions {
        public static String NAMESPACE = "http://graphs.azurewebsites.net/api/subscriptions/";
        public static String PREFIX = "subs";
        public static Namespace NS = EntityNamespace.of(PREFIX, NAMESPACE);
    }

}
