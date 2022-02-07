package com.bechtle.eagl.graph.domain.model.errors;

import org.eclipse.rdf4j.model.Resource;


public class MissingType extends Exception {
    private final String identifier;

    public MissingType(String identifier) {
        this.identifier = identifier;
    }

    public MissingType(Resource identifier) {
        this(identifier.stringValue());
    }

    @Override
    public String getMessage() {
        return "Entity with id '"+this.identifier+"' in request is missing a type definition.";
    }
}
