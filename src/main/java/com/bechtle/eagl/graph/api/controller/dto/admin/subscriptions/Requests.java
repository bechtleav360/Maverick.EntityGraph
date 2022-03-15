package com.bechtle.eagl.graph.api.controller.dto.admin.subscriptions;

public class Requests {

    public record CreateSubscriptionRequest() {}

    public record CreateApiKeyRequest(String name) {}
}
