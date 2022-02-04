package com.bechtle.eagl.graph.connector.rdf4j.model;

import com.bechtle.eagl.graph.connector.rdf4j.model.vocabulary.Default;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.base.AbstractIRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import java.net.URI;

public class EntityIdentifier extends AbstractIRI {


    private String namespace;
    private String localName;

    protected EntityIdentifier() {

    }

    public EntityIdentifier(String iri) {
        this(SimpleValueFactory.getInstance().createIRI(iri));
    }

    public static EntityIdentifier fromIdentifier(String identifier) {
        EntityIdentifier result = new EntityIdentifier();
        result.setNamespace(Default.NAMESPACE);
        result.setLocalName(identifier);
        return result;
    }

    public EntityIdentifier(String namespace, String localName) {
        this.namespace = namespace;
        this.localName = localName;
    }

    public EntityIdentifier(IRI iri) {
        this(iri.getNamespace(), iri.getLocalName());
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
