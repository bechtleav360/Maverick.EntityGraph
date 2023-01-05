package io.av360.maverick.graph.api.security;

import io.av360.maverick.graph.model.security.ApiKeyAuthenticationToken;
import io.av360.maverick.graph.model.security.Authorities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.DelegatingReactiveAuthenticationManager;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.security.web.server.authentication.ServerHttpBasicAuthenticationConverter;
import reactor.core.publisher.Mono;

import java.util.List;

import static io.av360.maverick.graph.model.security.ApiKeyAuthenticationToken.API_KEY_HEADER;

@Configuration
@EnableWebFluxSecurity
@Profile("! test")
@Slf4j(topic = "graph.config.security")
public class SecurityConfiguration {


    @Bean
    @ConditionalOnProperty(name = "application.security.enabled", havingValue = "true")
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                         List<ReactiveAuthenticationManager> authenticationManager,
                                                         ServerAuthenticationConverter authenticationConverter) {
        final ReactiveAuthenticationManager authenticationManagers = new DelegatingReactiveAuthenticationManager(authenticationManager);
        final AuthenticationWebFilter authenticationWebFilter = new AuthenticationWebFilter(authenticationManagers);

        authenticationWebFilter.setServerAuthenticationConverter(authenticationConverter);


        SecurityWebFilterChain build = http
                .authorizeExchange()

                .pathMatchers(HttpMethod.GET, "/api/**").hasAnyAuthority(Authorities.SYSTEM.getAuthority(), Authorities.APPLICATION.getAuthority(), Authorities.CONTRIBUTOR.getAuthority(), Authorities.READER.getAuthority())
                .pathMatchers(HttpMethod.HEAD, "/api/**").hasAnyAuthority(Authorities.SYSTEM.getAuthority(), Authorities.APPLICATION.getAuthority(), Authorities.CONTRIBUTOR.getAuthority(), Authorities.READER.getAuthority())
                .pathMatchers(HttpMethod.DELETE, "/api/**").hasAnyAuthority(Authorities.SYSTEM.getAuthority(), Authorities.APPLICATION.getAuthority(), Authorities.CONTRIBUTOR.getAuthority())
                .pathMatchers(HttpMethod.POST, "/api/**").hasAnyAuthority(Authorities.SYSTEM.getAuthority(), Authorities.APPLICATION.getAuthority(), Authorities.CONTRIBUTOR.getAuthority())
                .pathMatchers("/api/admin/**").hasAnyAuthority(Authorities.SYSTEM.getAuthority(), Authorities.APPLICATION.getAuthority())
                .pathMatchers(HttpMethod.GET, "/swagger-ui/*").permitAll()
                .matchers(EndpointRequest.to("env", "logfile", "loggers", "metrics", "scheduledTasks")).hasAuthority(Authorities.SYSTEM.getAuthority())
                .matchers(EndpointRequest.to("health")).permitAll()

                .anyExchange().permitAll()
                .and()
                .addFilterAt(authenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .httpBasic().disable()
                .csrf().disable()
                .formLogin().disable()
                .logout().disable()
                .build();

        log.trace("Security is enabled and was configured to secure all requests.");
        return build;

    }


    @Bean
    @ConditionalOnProperty(name = "application.security.enabled", havingValue = "true")
    ServerAuthenticationConverter buildAuthenticationConverter() {
        return exchange -> {
            List<String> apiKeys = exchange.getRequest().getHeaders().get(API_KEY_HEADER);
            if (apiKeys == null || apiKeys.size() == 0) {

                if(exchange.getRequest().getPath().value().startsWith("/api")) {
                    log.warn("API Request to path '{}' without an API Key Header", exchange.getRequest().getPath().value());
                    Authentication anonymous = new AnonymousAuthenticationToken("key", "anonymous",
                            AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
                    return Mono.just(anonymous);
                } else {
                    // lets fallback to the standard authentication (basic) (for actuators and others)
                    ServerHttpBasicAuthenticationConverter basicAuthenticationConverter = new ServerHttpBasicAuthenticationConverter();
                    return basicAuthenticationConverter.convert(exchange);
                }



            }

            log.trace("Found a valid api key in request, delegating authentication.");
            ApiKeyAuthenticationToken apiKeyToken = new ApiKeyAuthenticationToken(exchange.getRequest().getHeaders().toSingleValueMap());
            return Mono.just(apiKeyToken);
        };
    }


}
