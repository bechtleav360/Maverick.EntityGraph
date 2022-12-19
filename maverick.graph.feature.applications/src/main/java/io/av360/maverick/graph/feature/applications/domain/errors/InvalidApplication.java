package io.av360.maverick.graph.feature.applications.domain.errors;

public class InvalidApplication extends Throwable {
    private final String applicationId;

    public InvalidApplication(String applicationId) {

        this.applicationId = applicationId;
    }

    @Override
    public String getMessage() {
        return String.format("No application found for id '%s'", applicationId);
    }
}
