package org.av360.maverick.graph.feature.applications.services.vocab;

import org.av360.maverick.graph.model.rdf.EntityNamespace;
import org.av360.maverick.graph.model.rdf.LocalIRI;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

public class ApplicationTerms {


    public static final String NAMESPACE = "https://av360.org/schema#";
    public static final String PREFIX = "avs";
    public static final Namespace NS = EntityNamespace.of(PREFIX, NAMESPACE);
    public static final IRI TYPE = LocalIRI.from(NAMESPACE, "Application");
    public static final IRI CONFIGURATION_ITEM = LocalIRI.from(NAMESPACE, "ConfigurationItem");
    public static final IRI ACTIVE_FEATURE = LocalIRI.from(NAMESPACE, "hasFeature");
    public static final IRI HAS_API_KEY = LocalIRI.from(NAMESPACE, "hasApiKey");
    public static final IRI HAS_KEY = DC.IDENTIFIER;
    public static final IRI HAS_LABEL = RDFS.LABEL;
    public static final IRI IS_PERSISTENT = LocalIRI.from(NAMESPACE, "isPersistent");
    public static final IRI IS_PUBLIC = LocalIRI.from(NAMESPACE, "isPublic");

    public static final IRI HAS_CONFIGURATION = LocalIRI.from(NAMESPACE, "hasConfiguration");
    public static final IRI CONFIG_KEY = LocalIRI.from(NAMESPACE, "key");

    public static final IRI CONFIG_VALUE = LocalIRI.from(NAMESPACE, "val");

    public static final IRI CONFIG_FOR = LocalIRI.from(NAMESPACE,"configFor");
}
