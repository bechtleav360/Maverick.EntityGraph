package cougar.graph.api.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

public class AbstractController {


    protected Mono<Authentication> getAuthentication() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .switchIfEmpty(Mono.error(new AuthenticationCredentialsNotFoundException("Failed to acquire authentication from security context.")))
                .filter(Authentication::isAuthenticated)
                .switchIfEmpty(Mono.error(new InsufficientAuthenticationException("Request couldn't be authenticated.")));
    }

    protected String[] splitPrefixedIdentifier(String prefixedKey) {
        String[] property = prefixedKey.split("\\.");
        Assert.isTrue(property.length == 2, "Failed to extract prefix and label from path parameter " + prefixedKey);
        return property;
    }
}
