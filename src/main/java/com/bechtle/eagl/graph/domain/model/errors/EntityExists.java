package com.bechtle.eagl.graph.domain.model.errors;

import org.eclipse.rdf4j.model.Resource;

public class EntityExists extends Throwable {


    private final String identifier;

    public EntityExists(String identifier) {
        this.identifier = identifier;
    }

    public EntityExists(Resource obj) {
        this.identifier = obj.stringValue();
    }

    @Override
    public String getMessage() {
        return "Conflict: Entity with id '"+identifier+"' already exists.";
    }

}
