package org.av360.maverick.graph.feature.applications.model.events;

import org.av360.maverick.graph.feature.applications.model.domain.Subscription;

public class TokenRevokedEvent {
    private final Subscription token;

    public TokenRevokedEvent(Subscription token) {

        this.token = token;
    }

    public Subscription getToken() {
        return token;
    }
}
