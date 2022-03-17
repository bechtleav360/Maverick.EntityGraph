package com.bechtle.eagl.graph.api.security;

import com.bechtle.eagl.graph.domain.services.SubscriptionsService;
import org.eclipse.rdf4j.query.algebra.Str;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;


/**
 * An authentication based on a valid subscription (identified by the api key in the request header)
 */
public class SubscriptionAuthentication extends AbstractAuthenticationToken {

    private final SubscriptionsService.ApiKeyDefinition apiKeyDefinition;

    public SubscriptionAuthentication(SubscriptionsService.ApiKeyDefinition apiKeyDefinition) {
        // TODO (if needed): check enabled features through subscription to add individual authorities
        super(Collections.emptyList());

        this.apiKeyDefinition = apiKeyDefinition;
    }

    @Override
    public Object getCredentials() {
        return this.apiKeyDefinition.subscriptionKey();
    }

    @Override
    public Object getPrincipal() {
        return this.apiKeyDefinition;
    }

    @Override
    public boolean isAuthenticated() {
        return StringUtils.hasLength(this.getSubscription()) && StringUtils.hasLength(this.getApiKey().key());
    }

    public SubscriptionsService.NamedKey getApiKey() {
        return this.apiKeyDefinition.key();
    }

    public String getSubscription() {
        return this.apiKeyDefinition.subscriptionKey();
    }
}
