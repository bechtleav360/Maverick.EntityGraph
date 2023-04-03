package org.av360.maverick.graph.feature.applications.domain.events;

import org.av360.maverick.graph.feature.applications.domain.model.Subscription;

public class TokenCreatedEvent {
    private final Subscription token;

    public TokenCreatedEvent(Subscription token) {

        this.token = token;
    }
}
