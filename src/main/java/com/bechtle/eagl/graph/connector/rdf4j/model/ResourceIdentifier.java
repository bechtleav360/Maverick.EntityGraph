package com.bechtle.eagl.graph.connector.rdf4j.model;

import com.bechtle.eagl.graph.model.Identifier;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public record ResourceIdentifier(IRI resource) implements Identifier {

    @Override
    public String getLocalName() {
        return resource.getLocalName();
    }

    @Override
    public String getNamespace() {
        return resource.getNamespace();
    }

    @Override
    public String toString() {
        return resource.stringValue();
    }

    public static ResourceIdentifier of(String identifier) {
        return new ResourceIdentifier(SimpleValueFactory.getInstance().createIRI(identifier));
    }
}
