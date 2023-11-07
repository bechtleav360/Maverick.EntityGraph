package org.av360.maverick.graph.model.errors.runtime;

import org.av360.maverick.graph.model.errors.InvalidRequest;
import org.eclipse.rdf4j.model.Resource;
import org.springframework.http.HttpStatus;


public class MissingType extends InvalidRequest {
    private final String identifier;

    public MissingType(String identifier) {
        this.identifier = identifier;
    }

    public MissingType(Resource identifier) {
        this(identifier.stringValue());
    }

    @Override
    public String getMessage() {
        return "Entity with id '" + this.identifier + "' in request is missing a type definition.";
    }

    @Override
    public HttpStatus getStatusCode() {
        return HttpStatus.BAD_REQUEST;
    }
}
