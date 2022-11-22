package io.av360.maverick.graph.feature.applications.domain.events;

import io.av360.maverick.graph.feature.applications.domain.model.ApplicationToken;

public class TokenRevokedEvent {
    private ApplicationToken token;

    public TokenRevokedEvent(ApplicationToken token) {

        this.token = token;
    }
}
