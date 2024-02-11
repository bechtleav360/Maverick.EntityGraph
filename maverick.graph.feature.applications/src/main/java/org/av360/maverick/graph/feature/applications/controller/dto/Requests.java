package org.av360.maverick.graph.feature.applications.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.av360.maverick.graph.feature.applications.model.domain.ApplicationFlags;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public class Requests {


    @Schema(
            example = """
                    {
                      "label": "string",
                      "flags": {
                        "isPersistent": true,
                        "isPublic": true
                      },
                      "tags" : [],
                      "configuration": {
                        "detect_duplicates_frequency": "@daily",
                        "replace_identifiers_frequency": "@daily",
                        "assign_internal_types_frequency": "@daily",
                        "export_frequency": "@weekly"
                      }
                    }
                """
    )
    public record RegisterApplicationRequest(String label, ApplicationFlags flags, Set<String> tags, Map<String, Serializable> configuration) {
    }

    public record CreateApiKeyRequest(String label) {
    }

}
