package io.av360.maverick.graph.model.enums;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.http.MediaType;

public class RdfMimeTypes extends MediaType {

    public static final MediaType RDFJSON;
    public static final MediaType NTRIPLES;
    public static final MediaType RDFXML;
    public static final MediaType N3;
    public static final MediaType NQUADS;
    public static final MediaType TURTLE;
    public static final MediaType TURTLESTAR;
    public static final MediaType JSONLD;
    public static final MediaType BINARY;
    public static final String TURTLE_VALUE = "text/turtle";
    public static final String TURTLESTAR_VALUE = "text/x-turtlestar";
    public static final String RDFXML_VALUE = "application/rdf+xml";
    public static final String RDFJSON_VALUE = "application/rdf+json";
    public static final String NTRIPLES_VALUE = "application/n-triples";
    public static final String JSONLD_VALUE = "application/ld+json";
    public static final String BINARY_VALUE = "application/x-binary-rdf";
    public static final String NQUADS_VALUE = "application/n-quads";
    public static final String N3_VALUE = "text/n3";

    static {
        RDFJSON = new RdfMimeTypes(RDFFormat.RDFJSON);
        RDFXML = new RdfMimeTypes(RDFFormat.RDFXML);
        NTRIPLES = new RdfMimeTypes(RDFFormat.NTRIPLES);
        TURTLE = new RdfMimeTypes(RDFFormat.TURTLE);
        TURTLESTAR = new RdfMimeTypes(RDFFormat.TURTLESTAR);
        JSONLD = new RdfMimeTypes(RDFFormat.JSONLD);
        BINARY = new RdfMimeTypes(RDFFormat.BINARY);

        NQUADS = new RdfMimeTypes(RDFFormat.NQUADS);

        N3 = new RdfMimeTypes(RDFFormat.N3);

    }

    public RdfMimeTypes(RDFFormat type) {
        super(type.getDefaultMIMEType());
    }
}


