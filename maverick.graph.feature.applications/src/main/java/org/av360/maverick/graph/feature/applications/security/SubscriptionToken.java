package org.av360.maverick.graph.feature.applications.security;

import org.av360.maverick.graph.feature.applications.domain.model.Application;
import org.av360.maverick.graph.feature.applications.domain.model.Subscription;
import org.av360.maverick.graph.model.security.AdminToken;
import org.av360.maverick.graph.model.security.ApiKeyAuthenticationToken;

import javax.annotation.Nullable;


/**
 * An authentication based on a registered node (identified by the api key in the request header)
 */
public class SubscriptionToken extends ApiKeyAuthenticationToken {

    public static final String USER_AUTHORITY = "USER";



    private final Subscription subscriptionToken;
    private final Application application;

    private Application requestedApplication;



    public SubscriptionToken(@Nullable Subscription subscription, Application application) {
        this.subscriptionToken = subscription;
        this.application = application;
    }


    public static SubscriptionToken fromApiKeyAuthentication(ApiKeyAuthenticationToken apiKeyAuthenticationToken, Subscription subscription, Application application) {
        SubscriptionToken authentication = new SubscriptionToken(subscription, application);
        authentication.setAuthenticated(apiKeyAuthenticationToken.isAuthenticated());
        authentication.setDetails(apiKeyAuthenticationToken.getDetails());
        authentication.getAuthorities().addAll(apiKeyAuthenticationToken.getAuthorities());
        return authentication;
    }

    public static SubscriptionToken fromAdminToken(AdminToken systemAuthentication, Application application) {
        SubscriptionToken authentication = new SubscriptionToken(null, application);
        authentication.setAuthenticated(systemAuthentication.isAuthenticated());
        authentication.setDetails(systemAuthentication.getDetails());
        authentication.getAuthorities().addAll(systemAuthentication.getAuthorities());
        return authentication;
    }


    public Application getApplication() {
        return this.application;
    }

    public Subscription getSubscription() {
        return subscriptionToken;
    }

    public Application getRequestedApplication() {
        return requestedApplication;
    }

    public void setRequestedApplication(Application requestedApplication) {
        this.requestedApplication = requestedApplication;
    }
}
