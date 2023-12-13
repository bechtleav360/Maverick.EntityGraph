package org.av360.maverick.graph.model.errors.requests;

import org.av360.maverick.graph.model.errors.InvalidRequest;
import org.springframework.http.HttpStatus;

public class DetailNotFound extends InvalidRequest {
    private final String identifier;
    private final String valuePredicate;
    private final String valueIdentifier;


    public DetailNotFound(String identifier, String valuePredicate, String valueIdentifier) {
        this.identifier = identifier;
        this.valuePredicate = valuePredicate;
        this.valueIdentifier = valueIdentifier;
    }

    @Override
    public String getMessage() {
        return "Unknown value identifier in request for entity <%s> and predicate <%s>: %s".formatted(this.identifier, this.valuePredicate, this.valueIdentifier);
    }

    @Override
    public HttpStatus getStatusCode() {
        return HttpStatus.NOT_FOUND;
    }


}
