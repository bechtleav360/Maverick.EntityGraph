package io.av360.maverick.graph.feature.applications.domain.model;

public record ApplicationFlags(boolean isPersistent, boolean isPublic, String s3Host, String s3BucketId, String exportFrequency) {
}
