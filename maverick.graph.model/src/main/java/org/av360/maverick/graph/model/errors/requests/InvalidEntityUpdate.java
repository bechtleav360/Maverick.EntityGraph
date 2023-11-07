package org.av360.maverick.graph.model.errors.requests;

import org.av360.maverick.graph.model.errors.InvalidRequest;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

public class InvalidEntityUpdate extends InvalidRequest {
    private final String key;
    private final String reason;

    public InvalidEntityUpdate(Resource entityIdentifier, String reason) {
        if(entityIdentifier instanceof IRI iri)
            this.key = iri.getLocalName();
        else
            this.key = entityIdentifier.stringValue();
        this.reason = reason;
    }

    public InvalidEntityUpdate(String entityKey, String reason) {
        this.key = entityKey;
        this.reason = reason;
    }


    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder("Invalid update for entity with id '").append(key).append("'. ");
        if (StringUtils.hasLength(this.reason)) {

            sb.append(this.reason);
            if(! this.reason.endsWith(".")) sb.append(".");
        }
        return sb.toString();
    }

    @Override
    public HttpStatus getStatusCode() {
        return HttpStatus.BAD_REQUEST;
    }
}
