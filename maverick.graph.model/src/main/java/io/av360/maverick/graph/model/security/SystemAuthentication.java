package io.av360.maverick.graph.model.security;

import io.av360.maverick.graph.model.security.ApiKeyAuthenticationToken;

import java.util.Map;

public class SystemAuthentication extends ApiKeyAuthenticationToken {

    public SystemAuthentication(RequestDetails details) {
        super(details);
        super.setAuthenticated(true);
        super.grantAuthority(Authorities.SYSTEM);
    }

    public SystemAuthentication() {
        this(new RequestDetails(null, Map.of(), Map.of()));
    }
}
