package org.av360.maverick.graph.feature.applications.domain.model;

import org.eclipse.rdf4j.model.IRI;

public record Application(IRI iri, String label, String key, ApplicationFlags flags) {




}

