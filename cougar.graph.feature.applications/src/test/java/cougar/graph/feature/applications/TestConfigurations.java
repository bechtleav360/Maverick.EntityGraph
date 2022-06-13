package cougar.graph.feature.applications;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;


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
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        SecurityWebFilterChain build = http.csrf().disable()
                .authorizeExchange().anyExchange().permitAll()
                .and().build();

        log.trace("Security is disabled (for testing).");
        return build;

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
