package io.av360.maverick.graph.model.errors;

import org.eclipse.rdf4j.model.Resource;
import org.springframework.util.StringUtils;

public class InvalidEntityUpdate extends Exception {
    private final String key;
    private final String reason;

    public InvalidEntityUpdate(Resource entityIdentifier, String reason) {
        this.key = entityIdentifier.stringValue();
        this.reason = reason;
    }

    public InvalidEntityUpdate(String entityKey, String reason) {
        this.key = entityKey;
        this.reason = reason;
    }


    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder("Invalid update for entity with id '").append(key).append("'.");
        if (StringUtils.hasLength(this.reason)) {
            sb.append(this.reason).append(".");
        }
        return sb.toString();
    }
}
