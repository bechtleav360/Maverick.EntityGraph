package io.av360.maverick.graph.tests.config;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;

/**
 * Test configurat
 */
@TestConfiguration
@Profile("test")
@Slf4j(topic = "graph.test.cfg")
public class TestRepositoryConfig {


    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public Repository createRepository() {
        return new SailRepository(new MemoryStore());
    }


}
