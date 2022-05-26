package com.bechtle.cougar.graph.repository.rdf4j.config;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;

@Configuration
@Profile({"test", "dev", "default"})
@Slf4j
public class DefaultVolatileRepositoryConfiguration {

    @Bean("schema-storage")
    public Repository createSchemaRepository() throws IOException {
        log.debug("Initializing volatile schema repository");


        return new SailRepository(new MemoryStore());
    }

    @Bean("application-storage")
    public Repository createSubscriptionsRepository() throws IOException {
        log.debug("Initializing volative application repository");
        return new SailRepository(new MemoryStore());
    }
}
