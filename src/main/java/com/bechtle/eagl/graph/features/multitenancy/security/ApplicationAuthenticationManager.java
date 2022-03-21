package com.bechtle.eagl.graph.features.multitenancy.security;

import com.bechtle.eagl.graph.api.security.AdminAuthentication;
import com.bechtle.eagl.graph.api.security.ApiKeyToken;
import com.bechtle.eagl.graph.api.security.errors.NoSubscriptionFound;
import com.bechtle.eagl.graph.api.security.errors.RevokedApiKeyUsed;
import com.bechtle.eagl.graph.api.security.errors.UnknownApiKey;
import com.bechtle.eagl.graph.features.multitenancy.domain.ApplicationsService;
import com.bechtle.eagl.graph.features.multitenancy.domain.model.ApiKey;
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
                .switchIfEmpty(Mono.error(new UnknownApiKey(authentication.getApiKey())))
                .filter(ApiKey::active)
                .switchIfEmpty(Mono.error(new RevokedApiKeyUsed(authentication.getApiKey())))
                .map(ApplicationAuthentication::new)
                .map(auth -> {
                    log.trace("(Security) Request with Api Key '{}' (and label '{}') is active and belongs to subscription '{}'", auth.getApiKey().key(), auth.getApiKey().label(), auth.getSubscription().key());
                    return auth;
                })
                .switchIfEmpty(Mono.error(new NoSubscriptionFound(authentication.getApiKey())));

        // FIXME: build new authentication object with tenant authority, information which repos to use, etc.
    }
}
