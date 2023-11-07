package org.av360.maverick.graph.model.vocabulary;

import org.av360.maverick.graph.model.rdf.EntityNamespace;
import org.av360.maverick.graph.model.rdf.LocalIRI;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

import java.util.Set;

public class ICAL {


    public static final String NAMESPACE = "http://www.w3.org/2002/12/cal/ical#";
    public static final String PREFIX = "ical";
    public static final Namespace NS = EntityNamespace.of(PREFIX, NAMESPACE);
    public static final IRI VEVENT = LocalIRI.from(NAMESPACE, "Vevent");
    public static final IRI SUMMARY = LocalIRI.from(NAMESPACE, "summary");
    public static final IRI IDENTIFIER = LocalIRI.from(NAMESPACE, "uid");

    public ICAL() {
    }

    public static Set<IRI> getClassifierTypes() {
        return Set.of(

        );
    }

    public static Set<IRI> getIndividualTypes() {
        return Set.of(
                VEVENT
        );
    }

    public static Set<IRI> getCharacteristicProperties() {
        return Set.of(
                SUMMARY,
                IDENTIFIER
        );
    }
}
