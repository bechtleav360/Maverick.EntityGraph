package org.av360.maverick.graph.tests.config;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.context.RequestDetails;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.model.security.ChainingAuthenticationManager;
import org.av360.maverick.graph.model.util.PreAuthenticationWebFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
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

import java.io.Serializable;
import java.util.List;

@TestConfiguration
@EnableWebFluxSecurity
@Profile({"(test && api) && !secure"})
@Slf4j(topic = "graph.test.cfg")
public class TestSecurityConfig {



    @Bean
    @ConditionalOnProperty(name = "application.security.enabled", havingValue = "false")
    public SecurityWebFilterChain unsecurityWebFilterChain(List<PreAuthenticationWebFilter> earlyFilters, List<ReactiveAuthenticationManager> authenticationManager, ServerAuthenticationConverter authenticationConverter) {
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
    @ConditionalOnProperty(name = "application.security.enabled", havingValue = "true")
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                         List<ReactiveAuthenticationManager> authenticationManager,
                                                         ServerAuthenticationConverter authenticationConverter,
                                                         List<PreAuthenticationWebFilter> preFilterList) {
        final ReactiveAuthenticationManager authenticationManagers = new ChainingAuthenticationManager(authenticationManager);
        final AuthenticationWebFilter authenticationWebFilter = new AuthenticationWebFilter(authenticationManagers);
        authenticationWebFilter.setServerAuthenticationConverter(authenticationConverter);


        preFilterList.forEach(preFilter -> http.addFilterBefore(preFilter, SecurityWebFiltersOrder.AUTHENTICATION));
        http.addFilterAt(authenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION);
        http.httpBasic(spec -> spec.disable());
        http.csrf(spec -> spec.disable());
        http.formLogin(spec -> spec.disable());
        http.logout(spec -> spec.disable());


        http.authorizeExchange(spec ->

                spec.pathMatchers(HttpMethod.GET, "/api/**")
                        .hasAnyAuthority(Authorities.SYSTEM.getAuthority(), Authorities.APPLICATION.getAuthority(), Authorities.CONTRIBUTOR.getAuthority(), Authorities.READER.getAuthority())
                        .pathMatchers(HttpMethod.GET, "/api/**").hasAnyAuthority(Authorities.SYSTEM.getAuthority(), Authorities.APPLICATION.getAuthority(), Authorities.CONTRIBUTOR.getAuthority(), Authorities.READER.getAuthority())
                        .pathMatchers(HttpMethod.HEAD, "/api/**").hasAnyAuthority(Authorities.SYSTEM.getAuthority(), Authorities.APPLICATION.getAuthority(), Authorities.CONTRIBUTOR.getAuthority(), Authorities.READER.getAuthority())
                        .pathMatchers(HttpMethod.DELETE, "/api/**").hasAnyAuthority(Authorities.SYSTEM.getAuthority(), Authorities.APPLICATION.getAuthority(), Authorities.CONTRIBUTOR.getAuthority())
                        .pathMatchers(HttpMethod.POST, "/api/**").hasAnyAuthority(Authorities.SYSTEM.getAuthority(), Authorities.APPLICATION.getAuthority(), Authorities.CONTRIBUTOR.getAuthority())
                        .pathMatchers("/api/admin/**").hasAnyAuthority(Authorities.SYSTEM.getAuthority(), Authorities.APPLICATION.getAuthority())
                        .pathMatchers(HttpMethod.GET, "/swagger-ui/*").permitAll()
                        .pathMatchers(HttpMethod.GET, "/nav").permitAll()
                        //.matchers(EndpointRequest.to("env", "logfile", "loggers", "metrics", "scheduledTasks")).hasAuthority(Authorities.SYSTEM.getAuthority())
                        // .matchers(EndpointRequest.to("health", "info")).permitAll()
                        .anyExchange().permitAll()
        );





        log.info("Security is enabled and was configured to secure all requests.");
        return http.build();
    }


    @Bean
    @ConditionalOnProperty(name = "application.security.enabled", havingValue = "false")
    ServerAuthenticationConverter buildTestingAuthenticationConverter() {
        return exchange -> {
            RequestDetails details = RequestDetails.withRequest(exchange.getRequest());
            TestingAuthenticationToken testingAuthenticationToken = (TestingAuthenticationToken) createTestContext().getAuthentication().get();
            testingAuthenticationToken.setDetails(details);
            return Mono.just(testingAuthenticationToken);
        };
    }



    public static void addConfigurationDetail(SessionContext context, Serializable key, String value) {
        ((RequestDetails) context.getAuthenticationOrThrow().getDetails()).setConfiguration(key, value);
    }

    public static SessionContext createTestContext() {
        TestingAuthenticationToken testingAuthenticationToken = new TestingAuthenticationToken("test", "test", List.of(Authorities.SYSTEM));
        testingAuthenticationToken.setDetails(new RequestDetails().setPath("/api/entities"));

        return new SessionContext().setAuthentication(testingAuthenticationToken)
                .setAuthorized()
                .updateEnvironment(environment -> {
                    environment.setRepositoryType(RepositoryType.ENTITIES);
                });
    }

    public static SessionContext createAdminContext() {
        return new SessionContext().setSystemAuthentication()
                .setAuthorized()
                .updateEnvironment(environment -> {
                    environment.setRepositoryType(RepositoryType.ENTITIES);
                });
    }

    public static SessionContext createAnonymousContext() {
        AnonymousAuthenticationToken token = new AnonymousAuthenticationToken("key", "anonymous", List.of(Authorities.GUEST));
        token.setDetails(new RequestDetails().setPath("/api/entities"));
        return new SessionContext().setAuthentication(token)
                .setAuthorized()
                .updateEnvironment(environment -> {
                    environment.setRepositoryType(RepositoryType.ENTITIES);
                });
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
