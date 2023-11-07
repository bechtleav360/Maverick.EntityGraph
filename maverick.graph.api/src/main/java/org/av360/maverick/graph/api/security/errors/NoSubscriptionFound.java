package org.av360.maverick.graph.api.security.errors;

public class NoSubscriptionFound extends SecurityException {

    private final String apiKey;

    public NoSubscriptionFound(String apiKey) {
        this.apiKey = apiKey;
    }


    @Override
    public String getMessage() {
        return String.format("No valid subscription found for api key '%s'", this.apiKey);
    }
}
