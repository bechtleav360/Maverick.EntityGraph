package org.av360.maverick.graph.feature.applications.domain.errors;

import org.av360.maverick.graph.model.errors.InvalidRequest;
import org.springframework.http.HttpStatus;

public class InvalidApplication extends InvalidRequest {
    private final String applicationId;

    public InvalidApplication(String applicationId) {

        this.applicationId = applicationId;
    }

    @Override
    public String getMessage() {
        return String.format("No application found for identifier or label '%s'", applicationId);
    }

    @Override
    public HttpStatus getStatusCode() {
        return HttpStatus.BAD_REQUEST;
    }
}
