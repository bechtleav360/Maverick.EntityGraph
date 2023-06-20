package org.av360.maverick.graph.model.security;

import org.av360.maverick.graph.model.context.RequestDetails;

public class AdminAuthentication extends ApiKeyAuthenticationToken {

    public AdminAuthentication(RequestDetails details) {
        super(details);
        super.setAuthenticated(true);
        super.grantAuthority(Authorities.SYSTEM);
    }

    public AdminAuthentication() {
        this(new RequestDetails());
    }
}
