package com.bechtle.eagl.graph.subscriptions.domain.errors;

public class InvalidSubscription extends Throwable{
    private final String subscriptionId;

    public InvalidSubscription(String subscriptionId) {

        this.subscriptionId = subscriptionId;
    }

    @Override
    public String getMessage() {
        return String.format("No subscription found for id '%s'", subscriptionId);
    }
}
