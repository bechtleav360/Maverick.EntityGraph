package com.bechtle.eagl.graph.subscriptions.api.dto;

import java.util.List;

/**
 * See https://dev.to/brunooliveira/practical-java-16-using-jackson-to-serialize-records-4og4
 */
public class Responses {

    public record Subscription(String key, String label, boolean persistent) {

    }

    public record ApiKeyWithSubscription(String key, String issueDate, boolean active, Subscription subscription) {

    }

    public record ApiKey(String key, String issueDate, boolean active) {

    }

    public record SubscriptionWithKeys(String key, String label, boolean persistent, List<ApiKey> keys) {

    }
}
