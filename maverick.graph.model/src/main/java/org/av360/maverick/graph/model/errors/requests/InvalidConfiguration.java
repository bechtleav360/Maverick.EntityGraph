package org.av360.maverick.graph.model.errors.requests;

import org.av360.maverick.graph.model.errors.InvalidRequest;
import org.springframework.http.HttpStatus;

public class InvalidConfiguration extends InvalidRequest {


    private final String message;

    public InvalidConfiguration(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return "Invalid configuration: "+message;
    }

    @Override
    public HttpStatus getStatusCode() {
        return HttpStatus.METHOD_NOT_ALLOWED;
    }

    @Override
    public String getReasonPhrase() {
        return this.getStatusCode().getReasonPhrase();
    }
}
