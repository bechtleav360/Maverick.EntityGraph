package org.av360.maverick.graph.api.config;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.api.security.ext.ChainingAuthenticationManager;
import org.av360.maverick.graph.model.context.RequestDetails;
import org.av360.maverick.graph.model.security.ApiKeyAuthenticationToken;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.model.security.GuestToken;
import org.av360.maverick.graph.model.util.PreAuthenticationWebFilter;
import org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.security.web.server.authentication.ServerHttpBasicAuthenticationConverter;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.av360.maverick.graph.model.security.ApiKeyAuthenticationToken.API_KEY_HEADER;

@Configuration
@EnableWebFluxSecurity
@Profile("! test")  // see TestSecurityConfig in Test Module
@Slf4j(topic = "graph.ctrl.cfg.sec")
public class SecurityConfiguration {


    @SuppressWarnings("Convert2MethodRef")
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

                spec.pathMatchers(HttpMethod.GET, "/api/**").hasAnyAuthority(Authorities.SYSTEM.getAuthority(), Authorities.APPLICATION.getAuthority(), Authorities.CONTRIBUTOR.getAuthority(), Authorities.READER.getAuthority())
                        .pathMatchers(HttpMethod.GET, "/api/**").hasAnyAuthority(Authorities.SYSTEM.getAuthority(), Authorities.APPLICATION.getAuthority(), Authorities.CONTRIBUTOR.getAuthority(), Authorities.READER.getAuthority())
                        .pathMatchers(HttpMethod.HEAD, "/api/**").hasAnyAuthority(Authorities.SYSTEM.getAuthority(), Authorities.APPLICATION.getAuthority(), Authorities.CONTRIBUTOR.getAuthority(), Authorities.READER.getAuthority())
                        .pathMatchers(HttpMethod.DELETE, "/api/**").hasAnyAuthority(Authorities.SYSTEM.getAuthority(), Authorities.APPLICATION.getAuthority(), Authorities.CONTRIBUTOR.getAuthority())
                        .pathMatchers(HttpMethod.POST, "/api/**").hasAnyAuthority(Authorities.SYSTEM.getAuthority(), Authorities.APPLICATION.getAuthority(), Authorities.CONTRIBUTOR.getAuthority())
                        .pathMatchers("/api/admin/**").hasAnyAuthority(Authorities.SYSTEM.getAuthority(), Authorities.APPLICATION.getAuthority())
                        .pathMatchers(HttpMethod.GET, "/swagger-ui/*").permitAll()
                        .pathMatchers(HttpMethod.GET, "/nav").permitAll()
                        .matchers(EndpointRequest.to("env", "logfile", "loggers", "metrics", "scheduledTasks")).hasAuthority(Authorities.SYSTEM.getAuthority())
                        .matchers(EndpointRequest.to("health", "info")).permitAll()
                        .anyExchange().permitAll()
        );





        log.info("Security is enabled and was configured to secure all requests.");
        return http.build();
    }

    @SuppressWarnings("Convert2MethodRef")
    @Bean
    @ConditionalOnProperty(name = "application.security.enabled", havingValue = "false")
    public SecurityWebFilterChain unsecureWebFilterChain(ServerHttpSecurity http, List<ReactiveAuthenticationManager> authenticationManager, ServerAuthenticationConverter authenticationConverter, List<PreAuthenticationWebFilter> preFilterList) {
        final ReactiveAuthenticationManager authenticationManagers = new ChainingAuthenticationManager(authenticationManager);
        final AuthenticationWebFilter authenticationWebFilter = new AuthenticationWebFilter(authenticationManagers);
        authenticationWebFilter.setServerAuthenticationConverter(authenticationConverter);


        preFilterList.forEach(preFilter -> http.addFilterBefore(preFilter, SecurityWebFiltersOrder.AUTHENTICATION));
        http.addFilterAt(authenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION);
        http.authorizeExchange(spec -> spec.anyExchange().permitAll());
        http.httpBasic(spec -> spec.disable());
        http.csrf(spec -> spec.disable());
        http.formLogin(spec -> spec.disable());
        http.logout(spec -> spec.disable());


        log.info("Security is disabled.");
        return http.build();
    }


    @Bean
    @ConditionalOnProperty(name = "application.security.enabled", havingValue = "false")
    ServerAuthenticationConverter buildAnonymousAuthenticationConverter() {
        return exchange -> {
            RequestDetails details = RequestDetails.withRequest(exchange.getRequest());
            return Mono.just(new GuestToken(details));
        };
    }

    @Bean
    @ConditionalOnProperty(name = "application.security.enabled", havingValue = "true")
    ServerAuthenticationConverter buildAuthenticationConverter() {
        return exchange -> {
            RequestDetails details = RequestDetails.withRequest(exchange.getRequest());

            List<String> apiKeys = exchange.getRequest().getHeaders().get(API_KEY_HEADER);
            if (apiKeys == null || apiKeys.size() == 0) {

                if (exchange.getRequest().getPath().value().startsWith("/api")) {
                    log.trace("API Request to path '{}' without an API Key Header", exchange.getRequest().getPath().value());
                    AnonymousAuthenticationToken anonymous = new GuestToken(details);
                    return Mono.just(anonymous);
                }

                if (exchange.getRequest().getPath().value().startsWith("/actuator")) {
                    log.trace("API Request to path '{}' without an API Key Header", exchange.getRequest().getPath().value());
                    // lets fallback to the standard authentication (basic) (for actuators and others)
                    ServerHttpBasicAuthenticationConverter basicAuthenticationConverter = new ServerHttpBasicAuthenticationConverter();
                    return basicAuthenticationConverter.convert(exchange).map(authentication -> {
                        ((UsernamePasswordAuthenticationToken) authentication).setDetails(details);
                        return authentication;
                    });
                } else {
                    return Mono.just(new GuestToken(details));
                }
            } else {
                log.trace("Found a valid api key in request, delegating authentication.");
                ApiKeyAuthenticationToken apiKeyToken = new ApiKeyAuthenticationToken(details);
                return Mono.just(apiKeyToken);
            }
        };
    }


}
