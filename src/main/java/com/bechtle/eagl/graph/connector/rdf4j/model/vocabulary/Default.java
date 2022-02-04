package com.bechtle.eagl.graph.connector.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;

public class Default {

    public static final String NAMESPACE = "http://av360.io/schema/entities/";
    public static final String PREFIX = "base";
    public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);
}
