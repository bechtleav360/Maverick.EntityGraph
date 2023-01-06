package io.av360.maverick.graph.api.security;

import com.google.common.io.Files;
import io.av360.maverick.graph.model.security.ApiKeyAuthenticationToken;
import io.av360.maverick.graph.model.security.Authorities;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.RandomStringGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

@Component
@Slf4j(topic = "graph.config.security.default")
@Primary
public class DefaultAuthenticationManager implements ReactiveAuthenticationManager {

    @Value("${application.security.apiKey:}")
    String key;


    public DefaultAuthenticationManager() {
        log.trace("Activated Admin Authentication Manager (checking configured admin api key)");
    }

    @PostConstruct
    public void checkKey() {
        if (!StringUtils.hasLength(this.key)) {
            this.key = new RandomStringGenerator.Builder().withinRange('a', 'z').build().generate(16);
            log.info("No admin key set, using the following randomly generated api key for this session: '{}' ", this.key);
        } else {
            log.info("The following admin key will be used in this session: '{}' ", this.key);
        }
    }


    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        Assert.notNull(authentication, "Authentication is null in Authentication Manager");
        log.trace("Handling authentication of type {} in system authentication manager (default)", authentication.getClass().getSimpleName());

        if (authentication instanceof TestingAuthenticationToken) {
            log.warn("Test authentication token detected, disabling security.");
            authentication.setAuthenticated(true);
        }

        if (authentication instanceof AnonymousAuthenticationToken) {
            return handleAnonymousAuthentication(authentication)
                    .map(auth -> (Authentication) auth);
            //
            //authentication.setAuthenticated(false);
        }

        if (authentication instanceof UsernamePasswordAuthenticationToken) {
            return handleBasicAuthentication((UsernamePasswordAuthenticationToken) authentication)
                    .map(auth -> (Authentication) auth);
        }

        if (authentication instanceof ApiKeyAuthenticationToken) {
            return handleApiKeyAuthentication((ApiKeyAuthenticationToken) authentication)
                    .map(auth -> (Authentication) auth);

        }


        return Mono.just(authentication);
    }

    private Mono<? extends Authentication> handleAnonymousAuthentication(Authentication authentication) {
        log.info("Handling request with missing authentication, granting read-only access.");

        AnonymousAuthenticationToken auth = new AnonymousAuthenticationToken(authentication.getName(), authentication.getPrincipal(), List.of(Authorities.READER));
        auth.setAuthenticated(true);
        return Mono.just(auth);
    }

    private Mono<? extends Authentication> handleApiKeyAuthentication(ApiKeyAuthenticationToken authentication) {
        log.trace("Handling request with API Key authentication");
        // check if this is the admin user
        if (StringUtils.hasLength(this.key) && authentication.getApiKey().isPresent() && authentication.getApiKey().get().equalsIgnoreCase(this.key)) {
            log.debug("Valid System API Key for system authentication provided.");

            authentication.grantAuthority(Authorities.SYSTEM);
            authentication.setAuthenticated(true);
            return Mono.just(authentication);
        } else {
            log.trace("Invalid System API Key in request, denying access.");
        }

        return Mono.just(authentication);

    }

    /**
     * Some endpoints (e.g. the actuators) fall back to basic authentication.
     * <p>
     * For now, we expect only the system password here.
     *
     * @param authentication Current basic authentication
     * @return Authentication with relevant authorities
     */
    private Mono<? extends Authentication> handleBasicAuthentication(UsernamePasswordAuthenticationToken authentication) {
        log.trace("Handling request with basic authentication");

        if (StringUtils.hasLength(this.key) && StringUtils.hasLength(authentication.getCredentials().toString()) && authentication.getCredentials().toString().equalsIgnoreCase(this.key)) {
            log.debug("Valid password for system authentication provided.");


            UsernamePasswordAuthenticationToken authenticated = UsernamePasswordAuthenticationToken.authenticated(authentication.getPrincipal(), authentication.getPrincipal(), Set.of(Authorities.SYSTEM));
            return Mono.just(authenticated);
        }

        return Mono.just(authentication);
    }

}
