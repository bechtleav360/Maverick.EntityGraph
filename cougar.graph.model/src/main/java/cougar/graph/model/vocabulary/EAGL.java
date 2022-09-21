package cougar.graph.model.vocabulary;

import cougar.graph.model.rdf.EntityNamespace;
import org.eclipse.rdf4j.model.Namespace;

public class EAGL {



        public static final String NAMESPACE = "http://av360.io/schema/eagl/";
        public static final String PREFIX = "eagl";
        public static final Namespace NS = EntityNamespace.of(PREFIX, NAMESPACE);

        public EAGL() {
        }
}
