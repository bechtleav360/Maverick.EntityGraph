package org.av360.maverick.graph.model.security;

import org.av360.maverick.graph.model.context.RequestDetails;

public class SystemAuthentication extends ApiKeyAuthenticationToken {


    public SystemAuthentication() {
        this(Authorities.SYSTEM);
    }

    public SystemAuthentication(Authorities.WeightedAuthority authority) {
        super(new RequestDetails());
        super.setAuthenticated(true);
        super.grantAuthority(authority);
    }
}
