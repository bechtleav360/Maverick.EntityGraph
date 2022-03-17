package com.bechtle.eagl.graph.api.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.Collections;

public class ApiKeyAuthentication extends AbstractAuthenticationToken {

    private final String apiKey;

    public ApiKeyAuthentication(String apiKey) {
        super(Collections.emptyList());
        this.apiKey = apiKey;
    }

    @Override
    public Object getCredentials() {
        return this.getApiKey();
    }

    @Override
    public Object getPrincipal() {
        return "api_user";
    }

    public String getApiKey() {
        return this.apiKey;
    }
}