package org.av360.maverick.graph.feature.applications.services.model;

import org.av360.maverick.graph.feature.applications.config.Globals;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import java.io.Serializable;
import java.util.Map;

public record Application(IRI iri, String label, String key, ApplicationFlags flags,
                          Map<String, Serializable> configuration) {

    public enum CONFIG_KEYS {
        IDENTIFIER,
        LABEL,
        KEY,
        FLAG_PUBLIC,
        FLAG_PERSISTENT,
        FLAG_REMOTE
    }


    public static Application DEFAULT = new Application(
            SimpleValueFactory.getInstance().createIRI(Local.Applications.NAMESPACE, "default"),
            Globals.DEFAULT_APPLICATION_LABEL,
            "default",
            new ApplicationFlags(true, false, false),
            Map.of());


}

