package org.av360.maverick.graph.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import java.util.Set;

public class SKOSXL {

    public static final String NAMESPACE = "http://www.w3.org/2008/05/skos-xl#";
    public static final String PREFIX = "skos-xl";
    public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

    // CLASSES
    public static final IRI LABEL = SimpleValueFactory.getInstance().createIRI(NAMESPACE, "Label");

    // CONCEPTS
    public static final IRI ALT_LABEL = SimpleValueFactory.getInstance().createIRI(NAMESPACE, "altLabel");
    public static final IRI HIDDEN_LABEL = SimpleValueFactory.getInstance().createIRI(NAMESPACE, "hiddenLabel");
    public static final IRI LABEL_RELATION = SimpleValueFactory.getInstance().createIRI(NAMESPACE, "labelRelation");
    public static final IRI LITERAL_FORM = SimpleValueFactory.getInstance().createIRI(NAMESPACE, "literalForm");
    public static final IRI PREF_LABEL = SimpleValueFactory.getInstance().createIRI(NAMESPACE, "prefLabel");

    public static Set<IRI> getClassifierTypes() {
        return Set.of(
                LABEL
        );
    }

    public static Set<IRI> getIndividualTypes() {
        return Set.of();
    }

    public static Set<IRI> getCharacteristicProperties() {
        return Set.of(
                PREF_LABEL
        );
    }
}
