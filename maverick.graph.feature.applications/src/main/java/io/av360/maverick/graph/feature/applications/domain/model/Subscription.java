package io.av360.maverick.graph.feature.applications.domain.model;

import io.av360.maverick.graph.model.rdf.EntityNamespace;
import io.av360.maverick.graph.model.rdf.LocalIRI;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

/**
 * A subscription is valid for a specific application
 * @param iri
 * @param label
 * @param key
 * @param active
 * @param issueDate
 * @param application
 */
public record Subscription(IRI iri, String label, String key, boolean active, String issueDate, Application application) {



}