package io.av360.maverick.graph.model.enums;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.http.MediaType;

public class RdfMimeTypes  {

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
        RDFJSON = from(RDFFormat.RDFJSON);
        RDFXML = from(RDFFormat.RDFXML);
        NTRIPLES = from(RDFFormat.NTRIPLES);
        TURTLE = from(RDFFormat.TURTLE);
        TURTLESTAR = from(RDFFormat.TURTLESTAR);
        JSONLD = from(RDFFormat.JSONLD);
        BINARY = from(RDFFormat.BINARY);
        NQUADS = from(RDFFormat.NQUADS);
        N3 = from(RDFFormat.N3);
    }

    private static MediaType from(RDFFormat rdfFormat) {
        String[] split = rdfFormat.getDefaultMIMEType().split("/");
        if (split.length == 1) return new MediaType(split[0]);
        else return new MediaType(split[0], split[1]);
    }


}


