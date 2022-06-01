package com.bechtle.cougar.graph.api.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

public class AbstractController {
    @Value("${application.security.enabled:true}")
    boolean securityEnabled;


    protected Mono<Authentication> getAuthentication() {
        if(securityEnabled) {
            return ReactiveSecurityContextHolder.getContext()
                    .map(SecurityContext::getAuthentication)
                    .switchIfEmpty(Mono.error(new InternalAuthenticationServiceException("Failed to acquire authentication and security is enabled")));
        } else {
            return Mono.just(new TestingAuthenticationToken("test", "test"));
        }


    }

    protected String[] splitPrefixedIdentifier(String prefixedKey) {
        String[] property = prefixedKey.split("\\.");
        Assert.isTrue(property.length == 2, "Failed to extract prefix and label from path parameter " + prefixedKey);
        return property;
    }
}
