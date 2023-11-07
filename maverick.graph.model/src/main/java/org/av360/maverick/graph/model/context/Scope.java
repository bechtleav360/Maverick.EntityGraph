package org.av360.maverick.graph.model.context;

public record Scope(String label, Object details) {

    @Override
    public String toString() {
        return label;
    }
}
