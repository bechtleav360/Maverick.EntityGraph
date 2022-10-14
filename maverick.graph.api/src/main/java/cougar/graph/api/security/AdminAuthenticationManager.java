package cougar.graph.api.security;

import cougar.graph.model.security.ApiKeyAuthenticationToken;
import cougar.graph.model.security.Authorities;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.RandomStringGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;

@Component
@Slf4j(topic = "graph.config.security")
@Primary
public class AdminAuthenticationManager implements ReactiveAuthenticationManager {

    @Value("${application.security.apiKey:}")
    String key;


    public AdminAuthenticationManager() {
        log.trace("Activated Admin Authentication Manager (checking configured admin api key)");
    }

    @PostConstruct
    public void checkKey() {
        if(! StringUtils.hasLength(this.key)) {
            this.key = new RandomStringGenerator.Builder().withinRange('a', 'z').build().generate(16);
            log.info("No admin key set, using the following randomly generated api key for this session: '{}' ", this.key);
        } else {
            log.info("The following admin key will be used in this session: '{}' ", this.key);
        }
    }


    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        Assert.notNull(authentication, "Authentication is null in Authentication Manager");
        log.trace("(Filter) Handling authentication of type {} in Admin Authentication Manager ", authentication.getClass().getSimpleName());

        if(authentication instanceof TestingAuthenticationToken) {
            log.warn("Test authentication token detected, disabling security.");
            authentication.setAuthenticated(true);
        }

        if (authentication instanceof UsernamePasswordAuthenticationToken) {
            return handleBasicAuthentication((UsernamePasswordAuthenticationToken) authentication)
                    .map(auth -> (Authentication) auth);
        }

        if (authentication instanceof ApiKeyAuthenticationToken) {
            return handleApiKeyAuthentication((ApiKeyAuthenticationToken) authentication)
                    .map(auth -> (Authentication) auth);

        }


        return Mono.just(authentication);
    }

    private Mono<? extends Authentication> handleApiKeyAuthentication(ApiKeyAuthenticationToken authentication) {
        log.trace("Handling request with API Key authentication");
        // check if this is the admin user
        if(StringUtils.hasLength(this.key) && authentication.getApiKey().isPresent() && authentication.getApiKey().get().equalsIgnoreCase(this.key)) {
            log.debug("Valid API Key for Admin authentication provided.");

            authentication.grantAuthority(Authorities.SYSTEM);
            authentication.setAuthenticated(true);
            return Mono.just(authentication);
        }

        return Mono.just(authentication);

    }

    private Mono<? extends Authentication> handleBasicAuthentication(UsernamePasswordAuthenticationToken authentication) {
        return Mono.just(authentication);
    }

}
