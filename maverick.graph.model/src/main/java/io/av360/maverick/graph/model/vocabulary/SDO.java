package io.av360.maverick.graph.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public class SDO {


    private static final ValueFactory vf = SimpleValueFactory.getInstance();


    public static final String NAMESPACE = "https://schema.org/";
    public static final String PREFIX = "sdo";
    public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

    public static final IRI DEFINED_TERM = vf.createIRI(NAMESPACE, "DefinedTerm");
    public static final IRI VIDEO_OBJECT = vf.createIRI(NAMESPACE, "VideoObject");

    public static final IRI HAS_DEFINED_TERM = vf.createIRI(NAMESPACE, "hasDefinedTerm");
    public static final IRI IDENTIFIER = vf.createIRI(NAMESPACE, "identifier");

    public static final IRI TITLE = vf.createIRI(NAMESPACE, "title");
    public SDO() {
    }
}
