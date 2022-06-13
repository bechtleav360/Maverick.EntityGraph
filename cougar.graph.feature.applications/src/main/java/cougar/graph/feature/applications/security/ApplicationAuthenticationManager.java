package cougar.graph.feature.applications.security;

import cougar.graph.feature.applications.domain.ApplicationsService;
import cougar.graph.model.security.Authorities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import cougar.graph.model.security.ApiKeyAuthenticationToken;
import cougar.graph.api.security.errors.NoSubscriptionFound;

/**
 * This authentication manager augments the default admin authentication manager (which assumes only one api key exists).
 * <p>
 * It checks whether the given api key is part of an active application. Each application has its own repositories, you
 * can only access the data linked to the api key.
 *
 * It does not check for the validity of the admin key (this is still responsibility of the admin authentication manager).
 * This class must not give Admin Authority.
 */
@Component
@Slf4j
public class ApplicationAuthenticationManager implements ReactiveAuthenticationManager {



    private static final String SUBSCRIPTION_KEY_HEADER = "X-SUBSCRIPTION-KEY";

    private final ApplicationsService subscriptionsService;

    public ApplicationAuthenticationManager(ApplicationsService subscriptionsService) {
        this.subscriptionsService = subscriptionsService;
    }


    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        Assert.notNull(authentication, "Authentication is null in Authentication Manager");

        /*
            we have to assume that the admin authentication comes later
         */
        if (authentication instanceof ApiKeyAuthenticationToken) {
            // default case, we only asserted before that a valid api key was in the header
            return handleApiKeyHader((ApiKeyAuthenticationToken) authentication)
                            .map(auth -> this.handleSubscriptionKeyHeader((ApiKeyAuthenticationToken) auth))
                            .map(auth -> (Authentication) auth);
        } else {
            // fallback to default application (which is always declined)
            log.warn("Invalid authentication of type {} found and refused", authentication.getClass());
            authentication.setAuthenticated(false);
            return Mono.just(authentication);
        }

    }


    private Mono<? extends Authentication> handleApiKeyHader(ApiKeyAuthenticationToken apiKeyAuthenticationToken) {
        Assert.isAssignable(Authentication.class, ApiKeyAuthenticationToken.class);

        String apiKey = apiKeyAuthenticationToken.getApiKey().orElseThrow();
        return this.subscriptionsService.getKey(apiKey, apiKeyAuthenticationToken)
                .map(applicationApiKey -> {
                    log.debug("(Security) Valid api key for application '{}' provided in header '{}'.", applicationApiKey.label(), ApiKeyAuthenticationToken.API_KEY_HEADER);
                    ApplicationAuthenticationToken applicationAuthenticationToken = new ApplicationAuthenticationToken(apiKeyAuthenticationToken, applicationApiKey);
                    applicationAuthenticationToken.grantAuthority(Authorities.USER);
                    applicationAuthenticationToken.setAuthenticated(true);
                    return apiKeyAuthenticationToken;

                })
                .switchIfEmpty(Mono.just(apiKeyAuthenticationToken));

    }

    /**
     * Check if an additional application api key is in the headers. If yes, we try to find the associated application and
     * inject it into the authentication token for later use.
     * <p>
     * Note that this does not include any authorization
     *
     * @param apiKeyAuthenticationToken, the admin authentication
     * @return the consumed authentication
     */
    private Mono<? extends Authentication> handleSubscriptionKeyHeader(ApiKeyAuthenticationToken apiKeyAuthenticationToken) {
        //
        String subscriptionApiKey = apiKeyAuthenticationToken.getDetails().get(SUBSCRIPTION_KEY_HEADER);
        if (StringUtils.hasLength(apiKeyAuthenticationToken.getDetails().get(SUBSCRIPTION_KEY_HEADER))) {
            log.trace("Headers include a application key, trying to retrieve application details from storage.");
            // check if we can find the api key in one of our subscriptions
            return this.subscriptionsService.getKey(subscriptionApiKey, apiKeyAuthenticationToken)
                    .map(application -> {
                        log.debug("(Security) Valid api key for application '{}' provided in header '{}'.", application.label(), SUBSCRIPTION_KEY_HEADER);
                        return new ApplicationAuthenticationToken(apiKeyAuthenticationToken, application);
                    })
                    .switchIfEmpty(Mono.error(new NoSubscriptionFound(subscriptionApiKey)));
        } else {
            return Mono.just(apiKeyAuthenticationToken);
        }
    }


}
