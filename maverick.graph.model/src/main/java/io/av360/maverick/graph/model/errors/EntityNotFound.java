package io.av360.maverick.graph.model.errors;

import org.eclipse.rdf4j.model.IRI;

public class EntityNotFound extends Exception {
    private final String identifier;

    public EntityNotFound(String key) {
        this.identifier = key;
    }

    public EntityNotFound(IRI identifier) {
        this.identifier = identifier.getLocalName();
    }

    @Override
    public String getMessage() {
        return "Entity with id '" + identifier + "' does not exist.";
    }
}
