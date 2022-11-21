package io.av360.maverick.graph.feature.applications.domain.model;

import io.av360.maverick.graph.model.rdf.EntityNamespace;
import io.av360.maverick.graph.model.rdf.LocalIRI;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

public record Application(IRI iri, String label, String key, boolean persistent) {

    public static final String NAMESPACE = "http://av360.io/schema#";
    public static final String PREFIX = "sub";

    public static final Namespace NS = EntityNamespace.of(PREFIX, NAMESPACE);

    public static final IRI TYPE = LocalIRI.from(NAMESPACE, "Application");

    public static final IRI ACTIVE_FEATURE = LocalIRI.from(NAMESPACE, "hasFeature");
    public static final IRI HAS_API_KEY = LocalIRI.from(NAMESPACE, "hasApiKey");
    public static final IRI HAS_KEY = DC.IDENTIFIER;
    public static final IRI HAS_LABEL = RDFS.LABEL;
    public static final IRI IS_PERSISTENT = LocalIRI.from(NAMESPACE, "isPersistent");
    public static final IRI HAS_S3_HOST = LocalIRI.from(NAMESPACE, "hasS3Host");
    public static final IRI HAS_S3_BUCKET_ID = LocalIRI.from(NAMESPACE, "hasS3BucketId");
    public static final IRI HAS_EXPORT_FREQUENCY = LocalIRI.from(NAMESPACE, "hasExportFrequency");

}

