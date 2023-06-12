package org.av360.maverick.graph.model.context;

import org.springframework.security.core.Authentication;

import java.util.Objects;
import java.util.Optional;

public class SessionContext {
    RequestDetails requestDetails;
    Environment environment;
    Authentication authentication;
    Scope scope;

    public SessionContext() {
    }


    public Optional<RequestDetails> getRequestDetails() {
        return Optional.of(this.requestDetails);
    }

    public Optional<Environment> getEnvironment() {
        return Optional.of(this.environment);
    }

    public Optional<Authentication> getAuthentication() {
        return Optional.of(this.authentication);
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



    public String toString() {
        return "SessionContext(requestDetails=" + this.getRequestDetails() + ", environment=" + this.getEnvironment() + ", authentication=" + this.getAuthentication() + ", scope=" + this.getScope() + ")";
    }
}
