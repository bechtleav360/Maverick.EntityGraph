package cougar.graph.model.vocabulary;

import cougar.graph.model.rdf.EntityNamespace;
import org.eclipse.rdf4j.model.Namespace;

public class ESCO {



        public static final String NAMESPACE = "http://data.europa.eu/esco/model#";
        public static final String PREFIX = "esco";
        public static final Namespace NS = EntityNamespace.of(PREFIX, NAMESPACE);

        public ESCO() {
        }
}
