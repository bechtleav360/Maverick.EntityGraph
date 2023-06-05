package org.av360.maverick.graph.model.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;

import java.util.List;

public class GuestToken extends AnonymousAuthenticationToken {

    public GuestToken(RequestDetails details) {
        super("guest", "anonymous", List.of(Authorities.READER));
        this.setDetails(details);
    }

    public GuestToken() {
        this(new RequestDetails());
    }
}
