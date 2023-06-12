package org.av360.maverick.graph.feature.applications.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.av360.maverick.graph.feature.applications.services.model.ApplicationFlags;

import java.io.Serializable;
import java.util.Map;

public class Requests {


    @Schema(
            example = "{\"label\": \"string\", \"flags\": {\"isPersistent\": true, \"isPublic\": true}, \"configuration\": {\"detect_duplicates_frequency\": \"0 */5 * * * ?\", \"replace_identifiers_frequency\": \"0 */5 * * * ?\", \"assign_internal_types_frequency\": \"0 */5 * * * ?\", \"export_frequency\": \"0 */5 * * * ?\", \"export_local_path\": \"/\", \"export_s3_host\": \"http://localhost:9000\", \"export_s3_bucket\": \"string\"}}"
    )
    public record RegisterApplicationRequest(String label, ApplicationFlags flags, Map<String, Serializable> configuration) {
    }

    public record CreateApiKeyRequest(String label) {
    }

}
