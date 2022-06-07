package com.bechtle.cougar.graph.features.multitenancy.security;

import com.bechtle.cougar.graph.features.multitenancy.domain.model.ApiKey;
import com.bechtle.cougar.graph.features.multitenancy.domain.model.Application;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;


/**
 * An authentication based on a registered application (identified by the api key in the request header)
 */
public class ApplicationAuthentication extends AbstractAuthenticationToken {

    public static final String USER_AUTHORITY = "USER";


    private final ApiKey apiKey;

    public ApplicationAuthentication(ApiKey apiKey) {
        // TODO (if needed): check enabled features through application/subscription to add individual authorities
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

    public Application getSubscription() {
        return apiKey.subscription();
    }

    public ApiKey getApiKey() {
        return apiKey;
    }




}
