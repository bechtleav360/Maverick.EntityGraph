package io.av360.maverick.graph.model.errors;

import org.eclipse.rdf4j.model.Resource;

public class InvalidEntityModel extends Exception {
    private final String identifier;

    public InvalidEntityModel(String identifier) {
        this.identifier = identifier;
    }

    public InvalidEntityModel(Resource identifier) {
        this(identifier.stringValue());
    }

    @Override
    public String getMessage() {
        return "Entity with id '"+identifier+"' has an invalid model.";
    }
}
