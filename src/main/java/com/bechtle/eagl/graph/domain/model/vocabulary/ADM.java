package com.bechtle.eagl.graph.domain.model.vocabulary;

import com.bechtle.eagl.graph.domain.model.extensions.EntityNamespace;
import com.bechtle.eagl.graph.domain.model.extensions.LocalIRI;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.vocabulary.DC;

public class ADM {

    public static final String NAMESPACE = "http://av360.io/schema/admin/";
    public static final String PREFIX = "adm";
    public static final Namespace NS = EntityNamespace.of(PREFIX, NAMESPACE);

    public static final IRI SUBSCRIPTION = LocalIRI.from(NAMESPACE, "Subscription");
    public static final IRI SUBSCRIPTION_ACTIVE_FEATURE = LocalIRI.from(NAMESPACE, "hasSubscribedFeature");
    public static final IRI API_KEY = LocalIRI.from(NAMESPACE, "ApiKey");
    public static final IRI KEY_CREATION_DATE = LocalIRI.from(NAMESPACE, "issued");
    public static final IRI KEY_RECOVATION_DATE = LocalIRI.from(NAMESPACE, "revoked");
    public static final IRI KEY_IS_ACTIVE = LocalIRI.from(NAMESPACE, "active");
    public static final IRI SUBSCRIPTION_HAS_IDENTIFIER = DC.IDENTIFIER;
    public static final IRI KEY_HAS_IDENTIFIER = DC.IDENTIFIER;
    public static final IRI SUBSCRIPTION_API_KEY = LocalIRI.from(NAMESPACE, "hasApiKey");;

    public ADM() {
    }
}
