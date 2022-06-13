package cougar.graph.feature.applications.security;

import cougar.graph.feature.applications.domain.model.Application;
import cougar.graph.feature.applications.domain.model.ApplicationApiKey;
import cougar.graph.model.security.ApiKeyAuthenticationToken;


/**
 * An authentication based on a registered application (identified by the api key in the request header)
 */
public class ApplicationAuthenticationToken extends ApiKeyAuthenticationToken {

    public static final String USER_AUTHORITY = "USER";



    private final ApplicationApiKey applicationApiKey;


    public ApplicationAuthenticationToken(ApiKeyAuthenticationToken apiKeyAuthenticationToken, ApplicationApiKey application) {
        // TODO (if needed): check enabled features through application/application to add individual authorities
        super(apiKeyAuthenticationToken.getDetails());
        this.applicationApiKey = application;
        super.setAuthenticated(apiKeyAuthenticationToken.isAuthenticated());

    }


    public Application getSubscription() {
        return this.applicationApiKey.application();
    }

    public ApplicationApiKey getApplicationApiKey() {
        return applicationApiKey;
    }




}
