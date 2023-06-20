package org.av360.maverick.graph.model.context;

import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.model.security.SystemAuthentication;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class SessionContext {
    public static final SessionContext SYSTEM = new SessionContext().setSystemAuthentication();
    public static final SessionContext EMPTY = new SessionContext();
    RequestDetails requestDetails;
    Environment environment;
    Authentication authentication;
    Scope scope;
    private AuthorizationDecision decision;


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

    public SessionContext updateEnvironment(Consumer<Environment> customizer) {
        customizer.accept(this.getEnvironment());
        return this;
    }


    public Optional<Authentication> getAuthentication() {
        return Optional.ofNullable(this.authentication);
    }

    public Authentication getAuthenticationOrThrow() {
        if (this.authentication == null) throw new SecurityException("Missing authentication in request");
        else return this.authentication;
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




    public SessionContext setSystemAuthentication() {
        this.authentication = new SystemAuthentication();
        return this;
    }

    public SessionContext withAuthentication(Authentication authentication) {
        this.authentication = authentication;
        return this;
    }

    public SessionContext withAuthority(Authorities.WeightedAuthority authority) {
        this.authentication = new SystemAuthentication(authority);
        return this;
    }

    public boolean isScheduled() {
        return this.getRequestDetails().isEmpty();
    }

    public boolean isRequest() {
        return this.getRequestDetails().isPresent();
    }

    public SessionContext withAuthorization(AuthorizationDecision decision) {
        this.decision = decision;
        return this;
    }

    public SessionContext setAuthorized() {
        this.decision = new AuthorizationDecision(true);
        return this;
    }

    public AuthorizationDecision getDecision() {
        return decision;
    }



}
