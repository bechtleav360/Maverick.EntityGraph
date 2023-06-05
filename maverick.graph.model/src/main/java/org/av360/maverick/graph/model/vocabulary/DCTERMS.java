package org.av360.maverick.graph.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;

import java.util.Set;

public class DCTERMS extends org.eclipse.rdf4j.model.vocabulary.DCTERMS implements Vocabulary {

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
                DCTERMS.IDENTIFIER,
                DCTERMS.TITLE
        );
    }
}
