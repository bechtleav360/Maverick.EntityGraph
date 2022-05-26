package com.bechtle.cougar.graph.api.security;

import com.bechtle.cougar.graph.features.multitenancy.domain.model.ApiKey;
import lombok.Data;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;

/**
 * The initial authentication, only giving access to the Api Key itself. Is later replaced by the Subscription or
 * Admin authentication (depending on the key stored here).
 */
public class ApiKeyToken extends AbstractAuthenticationToken {



    public ApiKeyToken(String apiKey) {
        super(AuthorityUtils.NO_AUTHORITIES);
        super.setDetails(new ApiKeyDetails(apiKey));
    }

    @Override
    public void setDetails(Object details) {
        throw new NotImplementedException();
    }

    @Override
    public ApiKeyDetails getDetails() {
        return (ApiKeyDetails) super.getDetails();
    }

    public String getApiKey() {
        return this.getDetails().getApiKey();
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return this.getDetails().getApiKey();
    }


    @Data
    static class ApiKeyDetails {
        private final String apiKey;
        private String subscriptionKey;
        private ApiKey application;

        public ApiKeyDetails(String apiKey) {
            this.apiKey = apiKey;
        }

    }
}