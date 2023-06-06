package org.av360.maverick.graph.model.security;

public class AdminToken extends ApiKeyAuthenticationToken {

    public AdminToken(RequestDetails details) {
        super(details);
        super.setAuthenticated(true);
        super.grantAuthority(Authorities.SYSTEM);
    }

    public AdminToken() {
        this(new RequestDetails());
    }
}
