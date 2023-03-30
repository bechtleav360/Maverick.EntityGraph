package org.av360.maverick.graph.model.errors.requests;

import org.av360.maverick.graph.model.errors.InvalidRequest;
import org.eclipse.rdf4j.model.IRI;
import org.springframework.http.HttpStatus;

public class EntityNotFound extends InvalidRequest {
    private final String identifier;

    public EntityNotFound(String key) {
        this.identifier = key;
    }

    public EntityNotFound(IRI identifier) {
        this.identifier = identifier.getLocalName();
    }

    @Override
    public String getMessage() {
        return "Entity with id '" + identifier + "' does not exist.";
    }

    @Override
    public HttpStatus getStatusCode() {
        return HttpStatus.NOT_FOUND;
    }


}
