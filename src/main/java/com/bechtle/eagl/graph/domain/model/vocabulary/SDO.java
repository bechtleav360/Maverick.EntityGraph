package com.bechtle.eagl.graph.domain.model.vocabulary;

import com.bechtle.eagl.graph.domain.model.extensions.LocalIRI;
import com.bechtle.eagl.graph.domain.model.extensions.EntityNamespace;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public class SDO {

    private static ValueFactory vf = SimpleValueFactory.getInstance();


    public static final String NAMESPACE = "http://schema.org/";
    public static final String PREFIX = "sdo";
    public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

    public static final IRI DEFINED_TERM = vf.createIRI(NAMESPACE, "DefinedTerm");
    public static final IRI VIDEO_OBJECT = vf.createIRI(NAMESPACE, "VideoObject");

    public static final IRI HAS_DEFINED_TERM = vf.createIRI(NAMESPACE, "hasDefinedTerm");
    public static final IRI IDENTIFIER = vf.createIRI(NAMESPACE, "identifier");

    public SDO() {
    }
}
