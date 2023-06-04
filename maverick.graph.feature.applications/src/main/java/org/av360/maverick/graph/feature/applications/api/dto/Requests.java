package org.av360.maverick.graph.feature.applications.api.dto;

import org.av360.maverick.graph.feature.applications.domain.model.ApplicationFlags;

import java.io.Serializable;
import java.util.Map;

public class Requests {

    public record RegisterApplicationRequest(String label, ApplicationFlags flags, Map<String, Serializable> configuration) {
    }

    public record CreateApiKeyRequest(String label) {
    }

}
