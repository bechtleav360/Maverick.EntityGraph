package org.av360.maverick.graph.feature.applications.domain.model;

import io.swagger.v3.oas.annotations.media.Schema;

public record ApplicationFlags(boolean isPersistent,
                               boolean isPublic,
                               @Schema(description = "Frequency", example = "0 */5 * * * ?") String detectDuplicatesFrequency,
                               @Schema(description = "Frequency", example = "0 */5 * * * ?") String replaceIdentifiersFrequency,
                               @Schema(description = "Frequency", example = "0 */5 * * * ?") String typeCoercionFrequency,
                               @Schema(description = "Frequency", example = "0 */5 * * * ?") String exportFrequency,
                               @Schema(description = "S3 Host URI", example = "http://127.0.0.1:9000") String s3Host,
                               String s3BucketId
                               ) {
}
