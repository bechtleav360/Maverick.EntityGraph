package org.av360.maverick.graph.feature.admin.controller.dto;

import java.util.Map;

public record ImportFromEndpointRequest(String endpoint, Map<String, String> headers) {
}
