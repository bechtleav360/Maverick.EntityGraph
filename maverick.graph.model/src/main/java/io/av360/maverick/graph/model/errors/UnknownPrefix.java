package io.av360.maverick.graph.model.errors;

public class UnknownPrefix extends RuntimeException {
    private final String predicatePrefix;

    public UnknownPrefix(String predicatePrefix) {
        this.predicatePrefix = predicatePrefix;
    }

    @Override
    public String getMessage() {
        return String.format("The unknown prefix '%s' was provided in the request", this.predicatePrefix);
    }
}
