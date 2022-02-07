package com.bechtle.eagl.graph.repository.config;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.io.InputStream;

@Configuration
@Profile({"prod", "stage", "it", "local"})
public class RepositoryConfiguration {


    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public Repository createRepository() {
        return new SailRepository(new MemoryStore());
    }


    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public RepositoryConnection createRepositoryConnection(Repository repository) {
        return repository.getConnection();
    }
}
