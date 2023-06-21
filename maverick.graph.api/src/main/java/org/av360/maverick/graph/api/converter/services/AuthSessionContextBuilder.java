package org.av360.maverick.graph.api.converter.services;

import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.services.SessionContextBuilderService;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class AuthSessionContextBuilder implements SessionContextBuilderService {
    @Override
    public Mono<SessionContext> build(SessionContext context) {
        if(context.getAuthentication().isPresent()) {
            return Mono.just(context);
        } else {
            return ReactiveSecurityContextHolder.getContext()
                    .map(SecurityContext::getAuthentication)
                    .switchIfEmpty(Mono.error(new AuthenticationCredentialsNotFoundException("Failed to acquire authentication from security context.")))
                    .filter(Authentication::isAuthenticated)
                    .switchIfEmpty(Mono.error(new InsufficientAuthenticationException("Request couldn't be authenticated.")))
                    .map(context::withAuthentication);
        }


    }
}
