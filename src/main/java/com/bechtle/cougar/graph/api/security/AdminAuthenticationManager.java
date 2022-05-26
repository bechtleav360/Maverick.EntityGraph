package com.bechtle.cougar.graph.api.security;

import com.bechtle.cougar.graph.features.multitenancy.domain.ApplicationsService;
import com.bechtle.cougar.graph.api.security.errors.NoSubscriptionFound;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.RandomStringGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;

@Component
@Slf4j(topic = "cougar.graph.security")
@Primary
public class AdminAuthenticationManager implements ReactiveAuthenticationManager {

    @Value("${application.security.apiKey:}")
    String key;

    private final ApplicationsService subscriptionsService;

    public AdminAuthenticationManager(ApplicationsService subscriptionsService) {
        this.subscriptionsService = subscriptionsService;
    }

    @PostConstruct
    public void checkKey() {
        if(! StringUtils.hasLength(key)) {
            this.key = new RandomStringGenerator.Builder().withinRange('a', 'z').build().generate(16);
            log.info("No admin key set, using the following randomly generated api key for this session: '{}' ", this.key);
        }
    }


    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        Assert.notNull(authentication, "Authentication is null in Authentication Manager");

        if (authentication instanceof UsernamePasswordAuthenticationToken) {
            return handleBasicAuthentication((UsernamePasswordAuthenticationToken) authentication)
                    .map(auth -> (Authentication) auth);
        }

        if (authentication instanceof ApiKeyToken) {
            return handleApiKeyAuthentication((ApiKeyToken) authentication)
                    .map(auth -> (Authentication) auth);

        }


        return Mono.just(authentication);
    }

    private Mono<? extends Authentication> handleApiKeyAuthentication(ApiKeyToken authentication) {
        log.trace("Handling request with API Key authentication");
        // check if this is the admin user
        if(StringUtils.hasLength(this.key) && (authentication.getDetails().getApiKey().equalsIgnoreCase(this.key))) {
            log.debug("Valid API Key for Admin authentication provided.");
            AdminAuthentication adminAuthentication = new AdminAuthentication();
            adminAuthentication.setAuthenticated(true);


            if(StringUtils.hasLength(authentication.getDetails().getSubscriptionKey())) {
                log.trace("Headers include a subscription key, trying to retrieve application details from storage.");
                // check if we can find the api key in one of our subscriptions
                return this.subscriptionsService.getKey(authentication.getApiKey())
                        .map(application -> {
                            log.debug("Valid subscription key for application '{}' provided in admin authentication.", application.label());
                            adminAuthentication.getDetails().setApplication(application);
                            return adminAuthentication;
                        })
                        .switchIfEmpty(Mono.error(new NoSubscriptionFound(authentication.getApiKey())));
            } else {
                return Mono.just(adminAuthentication);
            }

        }



        return Mono.just(authentication);

    }

    private Mono<? extends Authentication> handleBasicAuthentication(UsernamePasswordAuthenticationToken authentication) {
        return Mono.just(authentication);
    }

}
