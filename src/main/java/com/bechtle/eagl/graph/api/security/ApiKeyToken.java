package com.bechtle.eagl.graph.api.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;

/**
 * The initial authentication, only giving access to the Api Key itself. Is later replaced by the Subscription or
 * Admin authentication (depending on the key stored here).
 */
public class ApiKeyToken extends AbstractAuthenticationToken {

    private String apiKey;

    public ApiKeyToken(String apiKey) {
        super(AuthorityUtils.NO_AUTHORITIES);
        this.apiKey = apiKey;
    }


    public String getApiKey() {
        return this.apiKey;
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return this.getApiKey();
    }
}