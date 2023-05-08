package org.av360.maverick.graph.api.security.ext;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.RandomStringGenerator;
import org.av360.maverick.graph.model.security.AdminToken;
import org.av360.maverick.graph.model.security.ApiKeyAuthenticationToken;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.model.security.GuestToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.Set;

@Component
@Slf4j(topic = "graph.ctrl.cfg.sec.default.mgr")
@Primary
public class DefaultAuthenticationManager implements ReactiveAuthenticationManager {

    @Value("${application.security.apiKey:}")
    String key;


    public DefaultAuthenticationManager() {
        log.trace("Activated default authentication manager (checking configured admin api key)");
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
        log.trace("Handling authentication of type '{}' and authentication status '{}' in default authentication manager ", authentication.getClass().getSimpleName(), authentication.isAuthenticated());


        if (authentication instanceof AnonymousAuthenticationToken token) {
            return handleAnonymousAuthentication(token)
                    .map(auth -> (Authentication) auth);
        } else

        if (authentication instanceof UsernamePasswordAuthenticationToken token) {
            return handleBasicAuthentication(token)
                    .map(auth -> (Authentication) auth);
        } else

        if (authentication instanceof ApiKeyAuthenticationToken token) {
            return handleApiKeyAuthentication(token)
                    .map(auth -> (Authentication) auth);

        } else {
            log.info("The authentication of type {} is not handled by default authentication manager", authentication.getClass());
            return Mono.just(authentication);
        }


    }

    private Mono<? extends Authentication> handleAnonymousAuthentication(AnonymousAuthenticationToken authentication) {
        log.info("Handling request with no authentication, granting read-only access in default authentication manager.");

        AnonymousAuthenticationToken auth = new GuestToken();
        auth.setAuthenticated(true);
        auth.setDetails(authentication.getDetails());
        return Mono.just(auth);
    }

    private Mono<? extends Authentication> handleApiKeyAuthentication(ApiKeyAuthenticationToken authentication) {
        log.trace("Handling request with API Key authentication in default authentication manager");
        // check if this is the admin userTransaction '
        if (StringUtils.hasLength(this.key) && authentication.getApiKey().isPresent() && authentication.getApiKey().get().equalsIgnoreCase(this.key)) {
            log.debug("Valid admin key provided, granting system access in default authentication manager.");

            return Mono.just(new AdminToken(authentication.getDetails()));
        } else {
            log.trace("Key in header is not the admin key, granting read-only access in default authentication manager.");
            authentication.grantAuthority(Authorities.READER);
            return Mono.just(authentication);
        }



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
        log.trace("Handling request with basic authentication in default authentication manager.");

        if (StringUtils.hasLength(this.key) && StringUtils.hasLength(authentication.getCredentials().toString()) && authentication.getCredentials().toString().equalsIgnoreCase(this.key)) {
            log.debug("Valid password in basic authentication checked by default authentication manager.");


            UsernamePasswordAuthenticationToken authenticated = UsernamePasswordAuthenticationToken.authenticated(authentication.getPrincipal(), authentication.getPrincipal(), Set.of(Authorities.SYSTEM));
            return Mono.just(authenticated);
        } else {
            log.warn("Given password for user '{}' is invalid, should be the configured admin token.", authentication.getPrincipal());
        }


        return Mono.just(authentication);
    }

}
