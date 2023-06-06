package org.av360.maverick.graph.tests.config;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.security.AdminToken;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.model.security.RequestDetails;
import org.av360.maverick.graph.model.util.PreAuthenticationWebFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.DelegatingReactiveAuthenticationManager;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.util.List;

@TestConfiguration
@EnableWebFluxSecurity
@Profile({"test && api"})
@Slf4j(topic = "graph.test.cfg")
public class TestSecurityConfig {


    public static void addConfigurationDetail(Authentication token, Serializable key, String value) {
        ((RequestDetails) token.getDetails()).setConfiguration(key, value);
    }

    @Bean
    @ConditionalOnProperty(name = "application.security.enabled", havingValue = "false")
    public SecurityWebFilterChain securityWebFilterChain(List<PreAuthenticationWebFilter> earlyFilters, List<ReactiveAuthenticationManager> authenticationManager, ServerAuthenticationConverter authenticationConverter) {
        final ReactiveAuthenticationManager authenticationManagers = new DelegatingReactiveAuthenticationManager(authenticationManager);
        final AuthenticationWebFilter authenticationWebFilter = new AuthenticationWebFilter(authenticationManagers);

        authenticationWebFilter.setServerAuthenticationConverter(authenticationConverter);

        ServerHttpSecurity http = ServerHttpSecurity.http();
        http.csrf(spec -> spec.disable());
        http.authorizeExchange(spec -> spec.anyExchange().permitAll());
        http.addFilterAt(authenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION);
        earlyFilters.forEach(filter -> http.addFilterBefore(filter, SecurityWebFiltersOrder.AUTHENTICATION));


        log.info("Security is disabled (for testing).");
        return http.build();

    }


    @Bean
    @ConditionalOnProperty(name = "application.security.enabled", havingValue = "false")
    ServerAuthenticationConverter buildTestingAuthenticationConverter() {
        return exchange -> {
            RequestDetails details = RequestDetails.withRequest(exchange.getRequest());
            TestingAuthenticationToken testingAuthenticationToken = createAuthenticationToken();
            testingAuthenticationToken.setDetails(details);
            return Mono.just(testingAuthenticationToken);
        };
    }




    public static TestingAuthenticationToken createAuthenticationToken() {
        TestingAuthenticationToken testingAuthenticationToken = new TestingAuthenticationToken("test", "test", List.of(Authorities.SYSTEM));
        testingAuthenticationToken.setDetails(new RequestDetails().setPath("/api/entities"));
        return testingAuthenticationToken;
    }

    public static AdminToken createAdminToken() {
        return new AdminToken();
    }

    public static AnonymousAuthenticationToken createAnonymousToken() {
        AnonymousAuthenticationToken token = new AnonymousAuthenticationToken("key", "anonymous", List.of(Authorities.GUEST));
        token.setDetails(new RequestDetails().setPath("/api/entities"));
        return token;
    }
    /*
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public TripleStore createTripleStore(Repository repository) {
        log.info("Creating triple store for testing. ");
        return new TripleStore(repository);
    }
    */
}
