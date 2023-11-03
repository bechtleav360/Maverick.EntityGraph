package org.av360.maverick.graph.model.errors.store;

import org.av360.maverick.graph.model.errors.InconsistentModelException;
import org.eclipse.rdf4j.model.Resource;
import org.springframework.util.StringUtils;

public class InvalidEntityModelException extends InconsistentModelException {
    private final String identifier;
    private String detail;

    public InvalidEntityModelException(String identifier) {
        this.identifier = identifier;
    }

    public InvalidEntityModelException(Resource identifier) {
        this(identifier.stringValue());
    }

    public InvalidEntityModelException(Resource identifier, String detail) {
        this(identifier.stringValue());
        this.detail = detail;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder("Invalid model for entity with id '").append(identifier).append("'.");
        if (StringUtils.hasLength(this.detail)) {
            sb.append(this.detail).append(".");
        }
        return sb.toString();
    }
}
