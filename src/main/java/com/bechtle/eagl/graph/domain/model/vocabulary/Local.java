package com.bechtle.eagl.graph.domain.model.vocabulary;

import com.bechtle.eagl.graph.domain.model.extensions.EntityNamespace;
import com.bechtle.eagl.graph.domain.model.extensions.LocalIRI;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * Constructed from the local domain. Constructed through configuration
 *
 * The prefixes here are also used to identify the responsible repository.
 */
public class Local {

    public static class Entities {
        public static String NAMESPACE = "http://graphs.azurewebsites.net/api/entities/";
        public static String PREFIX = "entity";
        public static Namespace NS = EntityNamespace.of(PREFIX, NAMESPACE);
    }


    public static class Transactions {
        public static String NAMESPACE = "http://graphs.azurewebsites.net/api/transactions/";
        public static String PREFIX = "transaction";
        public static Namespace NS = EntityNamespace.of(PREFIX, NAMESPACE);
    }

    public static class Versions {
        public static String NAMESPACE = "http://graphs.azurewebsites.net/api/versions/";
        public static String PREFIX = "version";
        public static Namespace NS = EntityNamespace.of(PREFIX, NAMESPACE);
    }

}
