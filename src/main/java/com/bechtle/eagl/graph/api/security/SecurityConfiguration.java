package com.bechtle.eagl.graph.api.security;

import org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import reactor.core.publisher.Mono;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
@Profile({"prod", "stage", "it", "dev", "persistent"})
public class SecurityConfiguration {


    private static final String API_KEY_HEADER = "X-API-KEY";




    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                         ReactiveAuthenticationManager authenticationManager,
                                                         ServerAuthenticationConverter authenticationConverter) {
        final AuthenticationWebFilter authenticationWebFilter = new AuthenticationWebFilter(authenticationManager);
        authenticationWebFilter.setServerAuthenticationConverter(authenticationConverter);

        return http
                .authorizeExchange()
                    .matchers(EndpointRequest.to("info","env", "logfile", "loggers", "metrics", "scheduledTasks")).hasAuthority(AdminAuthentication.ADMIN_AUTHORITY)
                    .matchers(EndpointRequest.to("health")).permitAll()
                    .pathMatchers("/api/admin/**").hasAuthority(AdminAuthentication.ADMIN_AUTHORITY)
                    .pathMatchers("/api/**").hasAuthority(SubscriptionAuthentication.USER_AUTHORITY)
                    .anyExchange().permitAll()
                .and()
                .addFilterAt(authenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .httpBasic().disable()
                .csrf().disable()
                .formLogin().disable()
                .logout().disable()
                .build();

    }





    @Bean
    ServerAuthenticationConverter buildAuthenticationConverter() {
        return exchange -> {
            List<String> headers = exchange.getRequest().getHeaders().get(API_KEY_HEADER);
            if (headers == null || headers.size() == 0) {
                // lets fallback to username/password
                return Mono.empty();
            }

            return Mono.just(new ApiKeyToken(headers.get(0)));
        };
    }





}
