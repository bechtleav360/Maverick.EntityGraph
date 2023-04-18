package org.av360.maverick.graph.feature.applications.api.dto;

import org.av360.maverick.graph.feature.applications.domain.model.ApplicationFlags;

import java.util.List;

/**
 * @see "https://dev.to/brunooliveira/practical-java-16-using-jackson-to-serialize-records-4og4"
 */
public class Responses {

    public record ApplicationResponse(String key, String label, ApplicationFlags flags) {

    }

    public record ApiKeyWithApplicationResponse(String key, String issueDate, boolean active,
                                                ApplicationResponse subscription) {

    }

    public record SubscriptionResponse(String key, String issueDate, boolean active) {

    }

    public record ApplicationWithApiKeys(String key, String label, ApplicationFlags flags, List<SubscriptionResponse> keys) {

    }
}