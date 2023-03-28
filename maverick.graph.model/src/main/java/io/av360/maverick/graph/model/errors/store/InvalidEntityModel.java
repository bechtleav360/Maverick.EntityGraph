package io.av360.maverick.graph.model.errors.store;

import io.av360.maverick.graph.model.errors.InconsistentModel;
import org.eclipse.rdf4j.model.Resource;
import org.springframework.util.StringUtils;

public class InvalidEntityModel extends InconsistentModel {
    private final String identifier;
    private String detail;

    public InvalidEntityModel(String identifier) {
        this.identifier = identifier;
    }

    public InvalidEntityModel(Resource identifier) {
        this(identifier.stringValue());
    }

    public InvalidEntityModel(Resource identifier, String detail) {
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
