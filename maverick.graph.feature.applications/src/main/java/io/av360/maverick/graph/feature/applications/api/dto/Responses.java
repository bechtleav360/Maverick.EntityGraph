package io.av360.maverick.graph.feature.applications.api.dto;

import java.util.Date;
import java.util.List;

/**
 * See https://dev.to/brunooliveira/practical-java-16-using-jackson-to-serialize-records-4og4
 */
public class Responses {

    public record ApplicationResponse(String key, String label, boolean persistent) {

    }

    public record ApiKeyWithApplicationResponse(String key, String issueDate, boolean active, ApplicationResponse subscription) {

    }

    public record ApiKeyResponse(String key, String issueDate, boolean active) {

    }

    public record ApplicationWithApiKeys(String key, String label, boolean persistent, List<ApiKeyResponse> keys) {

    }

    public record ApplicationConfigResponse(String s3Host, String s3BucketId, String exportFrequency) {

    }

    public record ExportResponse(String id) {

    }

    public record GetExportResponse(String id, String s3Host, String s3BucketId, String s3ObjectId) {

    }
}
