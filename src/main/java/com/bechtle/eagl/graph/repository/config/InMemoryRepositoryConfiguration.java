package com.bechtle.eagl.graph.repository.config;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.lmdb.LmdbStore;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Paths;

@Configuration
@Profile({"it", "local"})
public class InMemoryRepositoryConfiguration {



    @Bean("entities-storage")
    public Repository createEntitiesRepository() throws IOException {
        return new SailRepository(new MemoryStore());
    }


    @Bean("transactions-storage")
    public Repository createTransactionsRepository() throws IOException {
        return new SailRepository(new MemoryStore());
    }

    @Bean("schema-storage")
    public Repository createSchemaRepository() throws IOException {
        return new SailRepository(new MemoryStore());
    }
}
