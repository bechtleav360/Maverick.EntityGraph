package io.av360.maverick.graph.model.security;

import java.util.Map;

public class AdminToken extends ApiKeyAuthenticationToken {

    public AdminToken(RequestDetails details) {
        super(details);
        super.setAuthenticated(true);
        super.grantAuthority(Authorities.SYSTEM);
    }

    public AdminToken() {
        this(new RequestDetails(null, Map.of(), Map.of()));
    }
}
