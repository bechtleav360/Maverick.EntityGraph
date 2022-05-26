package com.bechtle.cougar.graph.features.multitenancy.domain.model;

import com.bechtle.cougar.graph.domain.model.extensions.EntityNamespace;
import com.bechtle.cougar.graph.domain.model.extensions.LocalIRI;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

public record ApiKey(IRI iri, String label, String key, boolean active, String issueDate, Application subscription) {

    public static final String NAMESPACE = Application.NAMESPACE;
    public static final String PREFIX = Application.PREFIX;
    public static final Namespace NS = EntityNamespace.of(PREFIX, NAMESPACE);

    public static final IRI TYPE = LocalIRI.from(NAMESPACE, "ApiKey");

    public static final IRI HAS_ISSUE_DATE = LocalIRI.from(NAMESPACE, "issued");
    public static final IRI HAS_REVOCATION_DATE = LocalIRI.from(NAMESPACE, "revoked");
    public static final IRI IS_ACTIVE = LocalIRI.from(NAMESPACE, "active");
    public static final IRI HAS_KEY = DC.IDENTIFIER;
    public static final IRI OF_SUBSCRIPTION = LocalIRI.from(NAMESPACE, "ofSubscription");;
    public static final IRI HAS_LABEL = RDFS.LABEL;

}