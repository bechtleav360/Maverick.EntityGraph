package com.bechtle.cougar.graph.features.multitenancy.security;

import com.bechtle.cougar.graph.api.security.ApiKeyToken;
import com.bechtle.cougar.graph.api.security.errors.NoSubscriptionFound;
import com.bechtle.cougar.graph.features.multitenancy.domain.ApplicationsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class ApplicationAuthenticationManager implements ReactiveAuthenticationManager  {

    private final ApplicationsService subscriptionsService;

    public ApplicationAuthenticationManager(ApplicationsService subscriptionsService) {
        this.subscriptionsService = subscriptionsService;
    }


    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        Assert.notNull(authentication, "Authentication is null in Authentication Manager");

        if(authentication.isAuthenticated()) return Mono.just(authentication);
        if (! (authentication instanceof ApiKeyToken)) return Mono.just(authentication);

        return handleApiKeyAuthentication((ApiKeyToken) authentication)
                .map(auth -> (Authentication) auth);
    }

    private Mono<? extends Authentication> handleApiKeyAuthentication(ApiKeyToken authentication) {
        // check if we can find the api key in one of our subscriptions
        return this.subscriptionsService.getKey(authentication.getApiKey())
                .map(ApplicationAuthentication::new)
                .map(auth -> {
                    log.trace("(Security) Request with Api Key '{}' (and label '{}') is active and belongs to subscription '{}'", auth.getApiKey().key(), auth.getApiKey().label(), auth.getSubscription().key());
                    return auth;
                })
                .switchIfEmpty(Mono.error(new NoSubscriptionFound(authentication.getApiKey())));

        // FIXME: build new authentication object with tenant authority, information which repos to use, etc.
    }
}
