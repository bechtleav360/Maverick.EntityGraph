package io.av360.maverick.graph.feature.applications.domain.events;

import io.av360.maverick.graph.feature.applications.domain.model.ApplicationToken;

public class TokenCreatedEvent {
    private ApplicationToken token;

    public TokenCreatedEvent(ApplicationToken token) {

        this.token = token;
    }
}
