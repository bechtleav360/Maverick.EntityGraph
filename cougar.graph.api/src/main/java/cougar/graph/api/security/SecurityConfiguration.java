package cougar.graph.api.security;

import cougar.graph.model.security.ApiKeyAuthenticationToken;
import cougar.graph.model.security.Authorities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
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

import static cougar.graph.model.security.ApiKeyAuthenticationToken.API_KEY_HEADER;

@Configuration
@EnableWebFluxSecurity
@Profile("! test")
@Slf4j(topic = "cougar.graph.security")
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
                .matchers(EndpointRequest.to("info", "env", "logfile", "loggers", "metrics", "scheduledTasks")).hasAuthority(Authorities.ADMIN.getAuthority())
                .matchers(EndpointRequest.to("health")).permitAll()
                .pathMatchers("/api/admin/**").hasAuthority(Authorities.ADMIN.getAuthority())
                .pathMatchers("/api/**").hasAnyAuthority(Authorities.USER.getAuthority(), Authorities.ADMIN.getAuthority())
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
                // lets fallback to username/password
                return Mono.empty();
            }

            log.trace("Found a valid api key in request, delegating authentication.");
            ApiKeyAuthenticationToken apiKeyToken = new ApiKeyAuthenticationToken(exchange.getRequest().getHeaders().toSingleValueMap());
            return Mono.just(apiKeyToken);
        };
    }

    @Bean
    @ConditionalOnProperty(name = "application.security.enabled", havingValue = "false")
    ServerAuthenticationConverter buildTestingAuthenticationConverter() {
        return exchange -> Mono.just(new TestingAuthenticationToken("test", "test"));
    }




}
