package com.bechtle.eagl.graph.model;

import com.bechtle.eagl.graph.model.vocabulary.Default;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.base.AbstractIRI;

public class SimpleIRI extends AbstractIRI {


    private String namespace;
    private String localName;

    protected SimpleIRI(String namespace, String localName) {
        this.namespace = namespace;
        this.localName = localName;
    }

    public SimpleIRI(IRI iri) {
        this(iri.getNamespace(), iri.getLocalName());
    }

    protected SimpleIRI(String namespace) {
        this.namespace = namespace;
    }

    public static SimpleIRI from(IRI iri) {
        return new SimpleIRI(iri.getNamespace(), iri.getLocalName());
    }


    public static SimpleIRI withDefaultNamespace(String localname) {
        return new SimpleIRI(Default.NAMESPACE, localname);
    }

    public static SimpleIRI withDefinedNamespace(String namespace, String localname) {
        return new SimpleIRI(namespace, localname);
    }



    protected void setLocalName(String localName) {
        this.localName = localName;
    }


    protected void setNamespace(String namespace) {
        this.namespace = namespace;
    }


    @Override
    public String getLocalName() {
        return this.localName;
    }

    @Override
    public String getNamespace() {
        return this.namespace;
    }



    @Override
    public boolean isResource() {
        return true;
    }

    @Override
    public boolean isLiteral() {
        return false;
    }

    @Override
    public boolean isTriple() {
        return false;
    }

    @Override
    public boolean isBNode() {
        return false;
    }

    @Override
    public boolean isIRI() {
        return true;
    }


}
