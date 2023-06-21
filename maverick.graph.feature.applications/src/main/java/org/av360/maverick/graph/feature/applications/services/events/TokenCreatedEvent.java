package org.av360.maverick.graph.feature.applications.services.events;

import org.av360.maverick.graph.feature.applications.services.model.Subscription;

public class TokenCreatedEvent {
    private final Subscription token;

    public TokenCreatedEvent(Subscription token) {

        this.token = token;
    }

    public Subscription getToken() {
        return token;
    }
}
