package io.av360.maverick.graph.api.security.errors;

public class UnknownApiKey extends SecurityException {
    private final String apiKey;

    public UnknownApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String getMessage() {
        return String.format("Unknown Api Key '%s'", this.apiKey);
    }
}
