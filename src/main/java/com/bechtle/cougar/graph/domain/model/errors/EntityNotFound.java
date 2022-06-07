package com.bechtle.cougar.graph.domain.model.errors;

public class EntityNotFound extends Exception {
    private final String identifier;

    public EntityNotFound(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String getMessage() {
        return "Entity with id '"+identifier+"' does not exist.";
    }
}
