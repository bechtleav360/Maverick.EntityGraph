package org.av360.maverick.graph.feature.applications.domain.vocab;

import org.av360.maverick.graph.model.rdf.EntityNamespace;
import org.av360.maverick.graph.model.rdf.LocalIRI;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

public class SubscriptionTerms {

    public static final String NAMESPACE = ApplicationTerms.NAMESPACE;
    public static final String PREFIX = ApplicationTerms.PREFIX;
    public static final Namespace NS = EntityNamespace.of(PREFIX, NAMESPACE);

    public static final IRI TYPE = LocalIRI.from(NAMESPACE, "SubscriptionToken");

    public static final IRI HAS_ISSUE_DATE = LocalIRI.from(NAMESPACE, "issued");
    public static final IRI HAS_REVOCATION_DATE = LocalIRI.from(NAMESPACE, "revoked");
    public static final IRI IS_ACTIVE = LocalIRI.from(NAMESPACE, "active");
    public static final IRI HAS_KEY = DC.IDENTIFIER;
    public static final IRI FOR_APPLICATION = LocalIRI.from(NAMESPACE, "forApplication");
    public static final IRI HAS_LABEL = RDFS.LABEL;
}
