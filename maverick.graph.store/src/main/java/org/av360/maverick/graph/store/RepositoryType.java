package org.av360.maverick.graph.store;

public enum RepositoryType {
    ENTITIES,
    SCHEMA,
    TRANSACTIONS,
    APPLICATION;


    @Override
    public String toString() {
        return name().toLowerCase();
    }




}