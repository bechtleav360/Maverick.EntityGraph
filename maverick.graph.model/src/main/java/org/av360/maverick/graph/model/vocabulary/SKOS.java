package org.av360.maverick.graph.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;

import java.util.Set;

public class SKOS extends org.eclipse.rdf4j.model.vocabulary.SKOS {


    public static Set<IRI> getClassifierTypes() {
        return Set.of(
                SKOS.CONCEPT

        );
    }

    public static Set<IRI> getIndividualTypes() {
        return Set.of(
                SKOS.COLLECTION,
                SKOS.ORDERED_COLLECTION
        );
    }

    public static Set<IRI> getCharacteristicProperties() {
        return Set.of(
                SKOS.PREF_LABEL,
                SKOS.DEFINITION
        );
    }
}
