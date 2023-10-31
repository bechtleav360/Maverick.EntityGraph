

package org.av360.maverick.graph.feature.applications.services.security;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.applications.config.ReactiveApplicationContextHolder;
import org.av360.maverick.graph.feature.applications.config.RequestedApplicationFilter;
import org.av360.maverick.graph.feature.applications.model.domain.Application;
import org.av360.maverick.graph.feature.applications.model.domain.SubscriptionToken;
import org.av360.maverick.graph.feature.applications.model.errors.InvalidApplication;
import org.av360.maverick.graph.feature.applications.services.ApplicationsService;
import org.av360.maverick.graph.feature.applications.services.SubscriptionsService;
import org.av360.maverick.graph.model.context.RequestDetails;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.security.AdminAuthentication;
import org.av360.maverick.graph.model.security.ApiKeyAuthenticationToken;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.model.util.StreamsLogger;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Optional;

/**
 * This authentication manager augments the default admin authentication manager (which assumes only one api key exists).
 * <p>
 * It checks whether the given api key is part of an active application. Each application has its own repositories, you
 * can only access the data linked to the api key.
 * <p>
 * It does not check for the validity of the admin key (this is still responsibility of the admin authentication manager).
 * This class must not give Admin Authority.
 */
@Component
@Slf4j(topic = "graph.feat.apps.sec.mgr")
public class ApplicationAuthenticationManager implements ReactiveAuthenticationManager {


    private static final String SUBSCRIPTION_KEY_HEADER = "X-SUBSCRIPTION-KEY";


    private final SubscriptionsService subscriptionsService;

    private final ApplicationsService applicationsService;

    public ApplicationAuthenticationManager(SubscriptionsService subscriptionsService, ApplicationsService applicationsService) {
        this.subscriptionsService = subscriptionsService;
        this.applicationsService = applicationsService;
        log.trace("Activated Application Authentication Manager (checking subscription api keys)");
    }





    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {

        try {
                Assert.notNull(authentication, "Authentication is null in Authentication Manager");
            if(authentication.isAuthenticated()) return Mono.just(authentication);


            return ReactiveApplicationContextHolder.getRequestedApplicationLabel()
                    .switchIfEmpty(this.checkPath(authentication))
                    .filter(StringUtils::hasLength)
                    .flatMap(label -> this.applicationsService.getApplicationByLabel(label, new SessionContext().withAuthentication(authentication))
                            .switchIfEmpty(Mono.error(new InvalidApplication(label))))
                    .flatMap(app -> this.addRequestedApplicationToAuthentication(app, authentication))
                    .flatMap(requestedApplication -> {
                        if (authentication instanceof AdminAuthentication token) {
                            log.trace("Authentication has system authority and application scope '{}' requested.", requestedApplication.label());
                            return Mono.just(SubscriptionToken.fromAdminToken(token, requestedApplication));

                        } else if (authentication instanceof ApiKeyAuthenticationToken token) {
                            log.trace("Key in header, checking if it is subscription token.");
                            return checkSubscriptionKeyForRequestedApplication(token, requestedApplication).map(auth -> auth);
                        } else {
                            return Mono.just(authentication);
                        }
                    })
                    .switchIfEmpty(defaultFallback(authentication))
                    .doOnSubscribe(StreamsLogger.trace(log, "Handling authentication of type '{}' and authentication status '{}' in application authentication manager ", authentication.getClass().getSimpleName(), authentication.isAuthenticated()));


        } catch (Exception e) {
            log.error("Failed to handle application authentication with error: '{}'", e.getMessage());
            return Mono.error(e);
        }
    }

    private Mono<String> checkPath(Authentication authentication) {
        return Mono.<String>create(sink -> {


            if(authentication.getDetails() instanceof RequestDetails requestDetails) {
                try {
                    Optional<String> requestedApplication = RequestedApplicationFilter.getRequestedApplicationFromRequestDetails(requestDetails);
                    requestedApplication.ifPresentOrElse(sink::success, sink::success);
                } catch (IOException e) {
                    sink.error(e);
                }
            } else {
                sink.success();
            }
        }).doOnSubscribe(subscription -> log.debug("Application label not found in thread context, verifying in request details within authentication."));



    }

    /**
     * We need to transport some configuration parameters downstream to the repository builders (and we don't want to use
     * the context, that's too brittle.
     *
     * @param application
     * @param authentication
     * @return
     */
    private Mono<Application> addRequestedApplicationToAuthentication(Application application, Authentication authentication) {
        if(authentication.getDetails() instanceof RequestDetails requestDetails) {
            requestDetails.setConfiguration(Application.CONFIG_KEYS.KEY, application.key());
            requestDetails.setConfiguration(Application.CONFIG_KEYS.LABEL.name(), application.label());
            requestDetails.setConfiguration(Application.CONFIG_KEYS.FLAG_PERSISTENT.name(), "%s".formatted(application.flags().isPersistent()));
            requestDetails.setConfiguration(Application.CONFIG_KEYS.FLAG_PUBLIC.name(), "%s".formatted(application.flags().isPublic()));
        } else {
            log.error("An application was requested, but no request details in the authentication object of type {}", authentication.getClass());
        }
        return Mono.just(application);

    }

    private Mono<Authentication> defaultFallback(Authentication authentication) {
        if (authentication instanceof AdminAuthentication token) {
            log.trace("Authentication has system authority, but not application scope requested.");
            return Mono.just(authentication);
        } else if (authentication instanceof ApiKeyAuthenticationToken token) {
            log.trace("Key in header and no requested application, checking if it is subscription token.");
            return this.checkSubscriptionKey(token).map(auth -> auth);

        } else if (authentication instanceof AnonymousAuthenticationToken token) {
            log.trace("Ignoring anonymous access without requested application in application authentication manager.");
            return Mono.just(authentication);
        } else {
            return Mono.just(authentication);
        }
    }

    /**
     * Tries to find a subscription for the given api key. If found, we assign the matching authority.
     *
     * @param token the token in the header
     * @return
     */
    protected Mono<? extends ApiKeyAuthenticationToken> checkSubscriptionKey(ApiKeyAuthenticationToken token) {
        if (token instanceof AdminAuthentication) return Mono.just(token);

        return this.subscriptionsService.getSubscription(token.getApiKey().orElseThrow(), new SessionContext().withAuthentication(token))
                .map(subscription -> {
                    SubscriptionToken subscriptionToken = SubscriptionToken.fromApiKeyAuthentication(token, subscription, subscription.application());
                    subscriptionToken.setRequestedApplication(subscription.application());
                    grant(subscriptionToken);
                    return subscriptionToken;
                })
                .flatMap(subscriptionToken -> Mono.just((ApiKeyAuthenticationToken) subscriptionToken))
                .switchIfEmpty(Mono.just(token));
    }

    protected Mono<? extends ApiKeyAuthenticationToken> checkSubscriptionKeyForRequestedApplication(ApiKeyAuthenticationToken token, Application requestedApplication) {
        if (token instanceof AdminAuthentication) return Mono.just(token);

        return this.subscriptionsService.getSubscription(token.getApiKey().orElseThrow(), new SessionContext().withAuthentication(token))
                .map(subscription -> {

                    SubscriptionToken st = SubscriptionToken.fromApiKeyAuthentication(token, subscription, subscription.application());
                    st.setRequestedApplication(requestedApplication);

                    // requested application matches subscription application
                    if (requestedApplication.label().equalsIgnoreCase(subscription.application().label())) {
                        grant(st);
                    }
                    // requested application does not match application for subscription
                    else {
                        // else we grant read access if public
                        if (st.getRequestedApplication().flags().isPublic()) {
                            log.trace("Valid subscription key for application '{}' unrelated to requested public application '{}' provided in header '{}'.", st.getApplication().label(), st.getRequestedApplication().label(), ApiKeyAuthenticationToken.API_KEY_HEADER);

                            st.grantAuthority(Authorities.READER);
                        } else {
                            log.debug("Valid subscription key for application '{}' unrelated to private application '{}' provided in header '{}'.", st.getApplication().label(), st.getRequestedApplication().label(), ApiKeyAuthenticationToken.API_KEY_HEADER);
                            st.purgeAuthorities();
                        }
                    }
                    return st;
                })
                .flatMap(subscriptionToken -> Mono.just((ApiKeyAuthenticationToken) subscriptionToken))
                .switchIfEmpty(Mono.just(token));

    }

    protected void grant(SubscriptionToken subscriptionToken) {
        if (subscriptionToken.getSubscription().active()) {
            subscriptionToken.setAuthenticated(true);

            // TODO: extract role from application key
            subscriptionToken.grantAuthority(Authorities.APPLICATION);
            log.trace("Valid subscription key for application '{}' provided in header '{}'.", subscriptionToken.getRequestedApplication().label(), ApiKeyAuthenticationToken.API_KEY_HEADER);
        } else {
            if (subscriptionToken.getRequestedApplication().flags().isPublic()) {
                subscriptionToken.setAuthenticated(true);
                subscriptionToken.grantAuthority(Authorities.READER);
                log.debug("Inactive subscription key for public application '{}' provided in header '{}'.", subscriptionToken.getRequestedApplication().label(), ApiKeyAuthenticationToken.API_KEY_HEADER);
            } else {
                subscriptionToken.setAuthenticated(false);
                subscriptionToken.purgeAuthorities();
                log.warn("Inactive subscription key for private application '{}' provided in header '{}'.", subscriptionToken.getRequestedApplication().label(), ApiKeyAuthenticationToken.API_KEY_HEADER);
            }
        }
    }


    /**
     * Returns the scope
     *
     * @param requestDetails request details such as path or headers
     * @return scope if available
     * @throws IOException if path is invalid
     */
    private Optional<String> getScopeFromPath(RequestDetails requestDetails) throws IOException {
        Assert.isTrue(StringUtils.hasLength(requestDetails.getPath()), "Empty path in request details");

        String[] split = requestDetails.getPath().split("/");
        for (int i = 0; i < split.length; i++) {
            if (split[i].equalsIgnoreCase("sc")) {
                if (split.length > i + 1) {
                    return Optional.of(split[i + 1]);
                } else {
                    throw new IOException("Invalid path in request, missing scope label: " + requestDetails.getPath());
                }
            }
        }

        return Optional.empty();
    }




}
