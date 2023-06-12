package org.av360.maverick.graph.api.controller;

import org.av360.maverick.graph.model.context.SessionContext;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;

public class AbstractController {


    // TODO: add ContextBuilderService, which is decorated by application feature

    protected Mono<Authentication> getAuthentication() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .switchIfEmpty(Mono.error(new AuthenticationCredentialsNotFoundException("Failed to acquire authentication from security context.")))
                .filter(Authentication::isAuthenticated)
                .switchIfEmpty(Mono.error(new InsufficientAuthenticationException("Request couldn't be authenticated.")));
    }


    protected Mono<SessionContext> buildRequestContext() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .switchIfEmpty(Mono.error(new AuthenticationCredentialsNotFoundException("Failed to acquire authentication from security context.")))
                .filter(Authentication::isAuthenticated)
                .switchIfEmpty(Mono.error(new InsufficientAuthenticationException("Request couldn't be authenticated.")))
                .map(authentication -> new SessionContext().setAuthentication(authentication));
    }




}
