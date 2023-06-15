package org.av360.maverick.graph.model.context;

import org.av360.maverick.graph.model.security.AdminToken;
import org.springframework.security.core.Authentication;

import java.util.Objects;
import java.util.Optional;

public class SessionContext {
    public static final SessionContext SYSTEM = new SessionContext().withSystemAuthentication();
    RequestDetails requestDetails;
    Environment environment;
    Authentication authentication;
    Scope scope;


    public SessionContext() {
    }


    public Optional<RequestDetails> getRequestDetails() {
        return Optional.ofNullable(this.requestDetails);
    }

    public Environment getEnvironment() {
        if (Objects.isNull(this.environment)) {
            this.environment = new Environment(this);
        }
        return this.environment;
    }

    public Environment withEnvironment() {
        return this.getEnvironment();
    }

    public Optional<Authentication> getAuthentication() {
        return Optional.ofNullable(this.authentication);
    }

    public Authentication getAuthenticationOrThrow() {
        if(this.authentication == null) throw new SecurityException("Missing authentication in request");
        else return this.authentication;
    }

    public Scope getScope() {
        return Objects.isNull(this.scope) ? new Scope("default", null) : this.scope;
    }

    public SessionContext setRequestDetails(RequestDetails requestDetails) {
        this.requestDetails = requestDetails;
        return this;
    }

    public SessionContext setEnvironment(Environment environment) {
        this.environment = environment;
        return this;
    }

    public SessionContext setAuthentication(Authentication authentication) {
        this.authentication = authentication;
        return this;
    }

    public SessionContext setScope(Scope scope) {
        this.scope = scope;
        return this;
    }




    public SessionContext withSystemAuthentication() {
        this.authentication = new AdminToken();
        return this;
    }

    public SessionContext withAuthentication(Authentication authentication) {
        this.authentication = authentication;
        return this;
    }
}
