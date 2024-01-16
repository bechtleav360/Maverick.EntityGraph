package org.av360.maverick.graph.feature.applications.controller.dto;

import org.av360.maverick.graph.feature.applications.model.domain.ApplicationFlags;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @see "https://dev.to/brunooliveira/practical-java-16-using-jackson-to-serialize-records-4og4"
 */
public class Responses {

    public record ApplicationResponse(String key, String label, ApplicationFlags flags, Set<String> tags, Map<String, Serializable> configuration) {

    }

    public record ApiKeyWithApplicationResponse(String key, String issueDate, boolean active,
                                                ApplicationResponse subscription) {

    }

    public record SubscriptionResponse(String key, String issueDate, boolean active) {

    }

    public record ApplicationWithApiKeys(String key, String label, ApplicationFlags flags, Set<String> tags, List<SubscriptionResponse> keys) {

    }

}
