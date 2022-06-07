package com.bechtle.cougar.graph.api.security.errors;

public class RevokedApiKeyUsed extends SecurityException {
    private final String apiKey;

    public RevokedApiKeyUsed(String apiKey) {
        this.apiKey = apiKey;
    }


    @Override
    public String getMessage() {
        return String.format("Api key '%s' has been revoked and cannot be used", this.apiKey);
    }

}
