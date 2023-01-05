package io.av360.maverick.graph.feature.applications.security;

import io.av360.maverick.graph.feature.applications.domain.model.Application;
import io.av360.maverick.graph.feature.applications.domain.model.ApplicationToken;
import io.av360.maverick.graph.model.security.ApiKeyAuthenticationToken;


/**
 * An authentication based on a registered application (identified by the api key in the request header)
 */
public class ApplicationAuthenticationToken extends ApiKeyAuthenticationToken {

    public static final String USER_AUTHORITY = "USER";


    private final ApplicationToken applicationToken;


    public ApplicationAuthenticationToken(ApiKeyAuthenticationToken apiKeyAuthenticationToken, ApplicationToken application) {
        // TODO (if needed): check enabled features through application/application to add individual authorities
        super(apiKeyAuthenticationToken.getDetails());
        this.applicationToken = application;
        super.setAuthenticated(apiKeyAuthenticationToken.isAuthenticated());

    }


    public Application getSubscription() {
        return this.applicationToken.application();
    }

    public ApplicationToken getApplicationApiKey() {
        return applicationToken;
    }


}
