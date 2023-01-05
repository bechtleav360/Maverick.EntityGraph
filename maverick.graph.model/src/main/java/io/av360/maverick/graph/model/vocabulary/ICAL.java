package io.av360.maverick.graph.model.vocabulary;

import io.av360.maverick.graph.model.rdf.EntityNamespace;
import io.av360.maverick.graph.model.rdf.LocalIRI;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

public class ICAL {


    public static final String NAMESPACE = "http://www.w3.org/2002/12/cal/ical#";
    public static final String PREFIX = "ical";
    public static final Namespace NS = EntityNamespace.of(PREFIX, NAMESPACE);
    public static final IRI VEVENT = LocalIRI.from(NAMESPACE, "VEVENT");

    public ICAL() {
    }
}
