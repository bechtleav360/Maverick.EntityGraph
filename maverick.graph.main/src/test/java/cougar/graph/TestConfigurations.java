package cougar.graph;

import cougar.graph.model.security.Authorities;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
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
@Profile("test")
@Slf4j
public class TestConfigurations {


    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public Repository createRepository() {
        return new SailRepository(new MemoryStore());
    }


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
        return exchange -> Mono.just(new TestingAuthenticationToken("test", "test", List.of(Authorities.SYSTEM)));
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
