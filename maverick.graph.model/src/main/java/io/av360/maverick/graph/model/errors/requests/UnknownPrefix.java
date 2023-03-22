package io.av360.maverick.graph.model.errors.requests;

import io.av360.maverick.graph.model.errors.InvalidRequest;
import org.springframework.http.HttpStatus;

public class UnknownPrefix extends InvalidRequest {
    private final String predicatePrefix;

    public UnknownPrefix(String predicatePrefix) {
        this.predicatePrefix = predicatePrefix;
    }

    @Override
    public String getMessage() {
        return String.format("The unknown prefix '%s' was provided in the request", this.predicatePrefix);
    }

    @Override
    public HttpStatus getStatusCode() {
        return HttpStatus.BAD_REQUEST;
    }
}
