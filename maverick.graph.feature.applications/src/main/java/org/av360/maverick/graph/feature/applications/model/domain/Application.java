package org.av360.maverick.graph.feature.applications.model.domain;

import org.av360.maverick.graph.feature.applications.config.Globals;
import org.av360.maverick.graph.model.vocabulary.meg.Local;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public record Application(IRI iri, String label, String key, Set<String> tags, ApplicationFlags flags,
                          Map<String, Serializable> configuration) {

    public enum CONFIG_KEYS {
        IDENTIFIER,
        LABEL,
        KEY,
        FLAG_PUBLIC,
        FLAG_PERSISTENT
    }


    public static Application DEFAULT = new Application(
            SimpleValueFactory.getInstance().createIRI(Local.Applications.NAME, "default"),
            Globals.DEFAULT_APPLICATION_LABEL,
            "default",
            Set.of(),
            new ApplicationFlags(false, false),
            Map.of());

}

