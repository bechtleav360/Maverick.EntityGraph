package org.av360.maverick.graph.feature.applications.api.dto;

import org.av360.maverick.graph.feature.applications.domain.model.ApplicationFlags;

public class Requests {

    public record RegisterApplicationRequest(String label, ApplicationFlags flags) {
    }

    public record CreateApiKeyRequest(String label) {
    }

}
