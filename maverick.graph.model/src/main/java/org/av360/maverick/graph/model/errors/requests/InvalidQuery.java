package org.av360.maverick.graph.model.errors.requests;

import org.av360.maverick.graph.model.errors.InvalidRequest;
import org.springframework.http.HttpStatus;

public class InvalidQuery extends InvalidRequest {
    private final String query;

    public InvalidQuery(String query) {
        this.query = query;
    }




    @Override
    public String getMessage() {
        return "Invalid query: %s".formatted(this.query);
    }

    @Override
    public HttpStatus getStatusCode() {
        return HttpStatus.BAD_REQUEST;
    }
}
