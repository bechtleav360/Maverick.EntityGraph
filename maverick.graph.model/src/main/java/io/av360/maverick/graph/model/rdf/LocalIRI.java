package io.av360.maverick.graph.model.rdf;

import io.av360.maverick.graph.model.vocabulary.Local;
import org.eclipse.rdf4j.model.base.AbstractIRI;

public class LocalIRI extends AbstractIRI {


    private String namespace;
    private String localName;

    protected LocalIRI(String namespace, String localName) {
        this.namespace = namespace;
        this.localName = localName;
    }

    public LocalIRI(org.eclipse.rdf4j.model.IRI iri) {
        this(iri.getNamespace(), iri.getLocalName());
    }

    protected LocalIRI(String namespace) {
        this.namespace = namespace;
    }

    public static LocalIRI from(org.eclipse.rdf4j.model.IRI iri) {
        return new LocalIRI(iri.getNamespace(), iri.getLocalName());
    }

    public static LocalIRI from(String namespace, String localName) {
        return new LocalIRI(namespace, localName);
    }

    public static LocalIRI withDefaultNamespace(String localname) {
        return new LocalIRI(Local.Entities.NAMESPACE, localname);
    }

    public static LocalIRI withDefinedNamespace(String namespace, String localname) {
        return new LocalIRI(namespace, localname);
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
