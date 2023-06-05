package org.av360.maverick.graph.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;

import java.util.Set;

public class DC extends org.eclipse.rdf4j.model.vocabulary.DC {

    public static Set<IRI> getClassifierTypes() {
        return Set.of(

        );
    }

    public  static Set<IRI> getIndividualTypes() {
        return Set.of(

        );
    }

    public static Set<IRI> getCharacteristicProperties() {
        return Set.of(
                DC.IDENTIFIER,
                DC.TITLE
        );
    }

}
