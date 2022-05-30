package com.bechtle.cougar.graph.domain.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.Authenticator;
import java.util.List;

public abstract class ServicesBase {

    @Value("${application.security.enabled:true}")
    boolean securityEnabled;


    public Mono<Authentication> getAuthentication() {
        if(securityEnabled) {
            return ReactiveSecurityContextHolder.getContext()
                    .map(SecurityContext::getAuthentication)
                    .switchIfEmpty(Mono.error(new InternalAuthenticationServiceException("Failed to acquire authentication and security is enabled")));
        } else {
            return Mono.just(new TestingAuthenticationToken("test", "test"));
        }


    }

}
