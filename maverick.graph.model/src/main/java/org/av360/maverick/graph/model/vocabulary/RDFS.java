package org.av360.maverick.graph.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;

import java.util.Set;

public class RDFS extends org.eclipse.rdf4j.model.vocabulary.RDFS {

    public static Set<IRI> getClassifierTypes() {
        return Set.of();
    }

    public  static Set<IRI> getIndividualTypes() {
        return Set.of();
    }

    public static Set<IRI> getCharacteristicProperties() {
        return Set.of(
                RDFS.LABEL
        );
    }
}
