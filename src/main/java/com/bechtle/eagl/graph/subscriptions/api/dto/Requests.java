package com.bechtle.eagl.graph.subscriptions.api.dto;

public class Requests {

    public record CreateSubscriptionRequest(boolean persistent, String label) {}

    public record CreateApiKeyRequest(String label) {}
}
