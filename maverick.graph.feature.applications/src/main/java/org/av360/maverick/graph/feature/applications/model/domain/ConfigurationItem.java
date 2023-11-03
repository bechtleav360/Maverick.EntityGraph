package org.av360.maverick.graph.feature.applications.model.domain;

import org.eclipse.rdf4j.model.IRI;

import java.io.Serializable;

public record ConfigurationItem(IRI node, String key, Serializable value, IRI appNode) {
}
