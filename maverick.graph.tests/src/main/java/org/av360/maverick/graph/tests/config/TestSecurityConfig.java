package org.av360.maverick.graph.tests.config;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.security.AdminToken;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.model.security.RequestDetails;
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
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import reactor.core.publisher.Mono;

import java.util.List;

@TestConfiguration
@EnableWebFluxSecurity
@Profile({"test && api"})
@Slf4j(topic = "graph.test.cfg")
public class TestSecurityConfig {


    @Bean
    @ConditionalOnProperty(name = "application.security.enabled", havingValue = "false")
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, List<ReactiveAuthenticationManager> authenticationManager, ServerAuthenticationConverter authenticationConverter) {
        final ReactiveAuthenticationManager authenticationManagers = new DelegatingReactiveAuthenticationManager(authenticationManager);
        final AuthenticationWebFilter authenticationWebFilter = new AuthenticationWebFilter(authenticationManagers);

        authenticationWebFilter.setServerAuthenticationConverter(authenticationConverter);

        SecurityWebFilterChain build = http.csrf().disable()
                .authorizeExchange().anyExchange().permitAll()
                .and().addFilterAt(authenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();

        log.info("Security is disabled (for testing).");
        return build;

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
        return new TestingAuthenticationToken("test", "test", List.of(Authorities.SYSTEM));
    }

    public static AdminToken createAdminToken() {
        return new AdminToken();
    }

    public static AnonymousAuthenticationToken createAnonymousToken() {
        return new AnonymousAuthenticationToken("key", "anonymous", List.of(Authorities.GUEST));
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
