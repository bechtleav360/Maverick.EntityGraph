package com.bechtle.eagl.graph.api.security;

import com.bechtle.eagl.graph.subscriptions.domain.SubscriptionsService;
import com.bechtle.eagl.graph.subscriptions.domain.model.ApiKey;
import com.bechtle.eagl.graph.subscriptions.domain.model.Subscription;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.util.StringUtils;


/**
 * An authentication based on a valid subscription (identified by the api key in the request header)
 */
public class SubscriptionAuthentication extends AbstractAuthenticationToken {

    public static final String USER_AUTHORITY = "USER";


    private final ApiKey apiKey;

    public SubscriptionAuthentication(ApiKey apiKey) {
        // TODO (if needed): check enabled features through subscription to add individual authorities
        super(AuthorityUtils.createAuthorityList(USER_AUTHORITY));
        this.apiKey = apiKey;

    }

    @Override
    public Object getCredentials() {
        return this.apiKey.key();
    }

    @Override
    public Object getPrincipal() {
        return this.apiKey.subscription();
    }

    @Override
    public boolean isAuthenticated() {
        return this.apiKey.active();
    }

    public Subscription getSubscription() {
        return apiKey.subscription();
    }

    public ApiKey getApiKey() {
        return apiKey;
    }




}
