package com.bechtle.eagl.graph.api.security;

import com.bechtle.eagl.graph.api.security.errors.NoSubscriptionFound;
import com.bechtle.eagl.graph.api.security.errors.RevokedApiKeyUsed;
import com.bechtle.eagl.graph.api.security.errors.UnknownApiKey;
import com.bechtle.eagl.graph.domain.services.SubscriptionsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.intercept.RunAsUserToken;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class SecurityAuthenticationManager implements ReactiveAuthenticationManager {

    @Value("${security.apiKey}")
    String key;

    private final SubscriptionsService subscriptionsService;

    public SecurityAuthenticationManager(SubscriptionsService subscriptionsService) {
        this.subscriptionsService = subscriptionsService;
    }


    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        Assert.notNull(authentication, "Authentication is null in Authentication Manager");

        if (authentication instanceof UsernamePasswordAuthenticationToken) {
            return handleBasicAuthentication((UsernamePasswordAuthenticationToken) authentication)
                    .map(auth -> (Authentication) auth);
        }

        if (authentication instanceof ApiKeyAuthentication) {
            return handleApiKeyAuthentication((ApiKeyAuthentication) authentication)
                    .map(auth -> (Authentication) auth);

        }


        return Mono.just(authentication);
    }

    private Mono<? extends Authentication> handleApiKeyAuthentication(ApiKeyAuthentication authentication) {
        // check if this is the admin user
        if(authentication.getApiKey().equalsIgnoreCase(this.key)) {

            RunAsUserToken runAsUserToken = new RunAsUserToken("admin", authentication.getApiKey(), authentication.getCredentials(), AuthorityUtils.createAuthorityList("ADMIN"), authentication.getClass());
            return Mono.just(runAsUserToken);
        }

        // check if we can find the api key in one of our subscriptions
        return this.subscriptionsService.getKey(authentication.getApiKey())
                .switchIfEmpty(Mono.error(new UnknownApiKey(authentication.getApiKey())))
                .filter(SubscriptionsService.ApiKeyDefinition::active)
                .switchIfEmpty(Mono.error(new RevokedApiKeyUsed(authentication.getApiKey())))
                .map(SubscriptionAuthentication::new)
                .map(auth -> {
                    log.trace("(Security) Request with Api Key '{}' (and name '{}') is active and belongs to subscription '{}'", auth.getApiKey().key(), auth.getApiKey().name(), auth.getSubscription());
                    return auth;
                })
                .switchIfEmpty(Mono.error(new NoSubscriptionFound(authentication.getApiKey())));

        // FIXME: build new authentication object with tenant authority, information which repos to use, etc.
    }

    private Mono<? extends Authentication> handleBasicAuthentication(UsernamePasswordAuthenticationToken authentication) {
        return Mono.just(authentication);
    }
}
