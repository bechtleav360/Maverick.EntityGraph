package com.bechtle.eagl.graph.domain.model.extensions;

import com.bechtle.eagl.graph.domain.model.vocabulary.Default;
import org.eclipse.rdf4j.model.base.AbstractIRI;

public class EntityIRI extends AbstractIRI {


    private String namespace;
    private String localName;

    protected EntityIRI(String namespace, String localName) {
        this.namespace = namespace;
        this.localName = localName;
    }

    public EntityIRI(org.eclipse.rdf4j.model.IRI iri) {
        this(iri.getNamespace(), iri.getLocalName());
    }

    protected EntityIRI(String namespace) {
        this.namespace = namespace;
    }

    public static EntityIRI from(org.eclipse.rdf4j.model.IRI iri) {
        return new EntityIRI(iri.getNamespace(), iri.getLocalName());
    }

    public static EntityIRI from(String namespace, String localName) {
        return new EntityIRI(namespace, localName);
    }

    public static EntityIRI withDefaultNamespace(String localname) {
        return new EntityIRI(Default.NAMESPACE, localname);
    }

    public static EntityIRI withDefinedNamespace(String namespace, String localname) {
        return new EntityIRI(namespace, localname);
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
