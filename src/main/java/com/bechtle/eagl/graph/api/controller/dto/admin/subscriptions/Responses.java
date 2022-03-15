package com.bechtle.eagl.graph.api.controller.dto.admin.subscriptions;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;
import java.util.Set;

/**
 * See https://dev.to/brunooliveira/practical-java-16-using-jackson-to-serialize-records-4og4
 */
public class Responses {

    public record Subscription(String identifier) {}


    public record SubscriptionWithKey(String identifier, List<String> key) {
    }
}
