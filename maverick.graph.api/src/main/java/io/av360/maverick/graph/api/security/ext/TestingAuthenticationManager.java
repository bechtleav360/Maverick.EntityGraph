package io.av360.maverick.graph.api.security.ext;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Profile("test | dev")
@Component
@Slf4j(topic = "graph.config.security.default")
public class TestingAuthenticationManager implements ReactiveAuthenticationManager {
    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        if (authentication instanceof TestingAuthenticationToken) {
            log.warn("Test authentication token detected, disabling security.");
            authentication.setAuthenticated(true);
            return Mono.just(authentication);
        } else {
            return Mono.just(authentication);
        }
    }
}
