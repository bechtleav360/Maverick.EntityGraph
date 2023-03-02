package io.av360.maverick.graph.feature.applications.domain.events;

import io.av360.maverick.graph.feature.applications.domain.model.Subscription;

public class TokenCreatedEvent {
    private Subscription token;

    public TokenCreatedEvent(Subscription token) {

        this.token = token;
    }
}
