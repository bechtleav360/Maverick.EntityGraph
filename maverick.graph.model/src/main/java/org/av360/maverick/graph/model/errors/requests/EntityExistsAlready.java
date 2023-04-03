package org.av360.maverick.graph.model.errors.requests;

import org.av360.maverick.graph.model.errors.InvalidRequest;
import org.eclipse.rdf4j.model.Resource;
import org.springframework.http.HttpStatus;

public class EntityExistsAlready extends InvalidRequest {


    private final String identifier;

    public EntityExistsAlready(String identifier) {
        this.identifier = identifier;
    }

    public EntityExistsAlready(Resource obj) {
        this.identifier = obj.stringValue();
    }

    @Override
    public String getMessage() {
        return "Conflict: Entity with id '" + identifier + "' already exists.";
    }

    @Override
    public HttpStatus getStatusCode() {
        return HttpStatus.CONFLICT;
    }

    @Override
    public String getReasonPhrase() {
        return this.getStatusCode().getReasonPhrase();
    }
}
