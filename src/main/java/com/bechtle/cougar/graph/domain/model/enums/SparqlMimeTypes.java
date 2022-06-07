package com.bechtle.cougar.graph.domain.model.enums;

import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.springframework.http.MediaType;

public class SparqlMimeTypes extends MediaType {

    public static final MediaType SPARQL;
    public static final MediaType SPARQL_STAR;
    public static final MediaType BINARY;
    public static final MediaType JSON;
    public static final MediaType JSON_STAR;
    public static final MediaType CSV;
    public static final MediaType TSV;
    public static final MediaType TSV_STAR;

    public static final String SPARQL_VALUE = "application/sparql-results+xml";
    public static final String SPARQL_STAR_VALUE = "application/x-sparqlstar-results+xml";
    public static final String BINARY_VALUE = "application/x-binary-rdf-results-table";
    public static final String JSON_VALUE = "application/sparql-results+json";
    public static final String JSON_STAR_VALUE = "application/x-sparqlstar-results+json";
    public static final String CSV_VALUE = "text/csv";
    public static final String TSV_VALUE = "text/tab-separated-values";
    public static final String TSV_STAR_VALUE = "text/x-tab-separated-values-star";


    static {
        SPARQL = new SparqlMimeTypes(TupleQueryResultFormat.SPARQL);
        SPARQL_STAR = new SparqlMimeTypes(TupleQueryResultFormat.SPARQL_STAR);
        BINARY = new SparqlMimeTypes(TupleQueryResultFormat.BINARY);
        JSON = new SparqlMimeTypes(TupleQueryResultFormat.JSON);
        JSON_STAR = new SparqlMimeTypes(TupleQueryResultFormat.JSON_STAR);
        CSV = new SparqlMimeTypes(TupleQueryResultFormat.CSV);
        TSV = new SparqlMimeTypes(TupleQueryResultFormat.TSV);
        TSV_STAR = new SparqlMimeTypes(TupleQueryResultFormat.TSV_STAR);


    }

    public SparqlMimeTypes(TupleQueryResultFormat type) {
        super(type.getDefaultMIMEType());
    }
}

