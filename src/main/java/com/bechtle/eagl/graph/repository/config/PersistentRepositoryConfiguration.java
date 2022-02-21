package com.bechtle.eagl.graph.repository.config;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.lmdb.LmdbStore;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbBNode;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Paths;

@Configuration
@Profile({"prod", "stage", "it", "persistent"})
@Slf4j
public class PersistentRepositoryConfiguration {



    @Bean("entities-storage")
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public Repository createEntitiesRepository(@Value("${storage.entities.path}") String storagePath) throws IOException {
        // FIXME:
        Resource file = new FileSystemResource(Paths.get(storagePath, "lmdb"));
        LmdbStoreConfig config = new LmdbStoreConfig();

        log.debug("Initializing persistent entity repository in path '{}'", file.getFile().toPath());

        return new SailRepository(new LmdbStore(file.getFile(), config));
    }


    @Bean("transactions-storage")
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public Repository createTransactionsRepository(@Value("${storage.transactions.path}") String storagePath) throws IOException {
        // FIXME:
        Resource file = new FileSystemResource(Paths.get(storagePath, "lmdb"));
        LmdbStoreConfig config = new LmdbStoreConfig();

        log.debug("Initializing persistent transactions repository in path '{}'", file.getFile().toPath());

        return new SailRepository(new LmdbStore(file.getFile(), config));
    }

    @Bean("schema-storage")
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public Repository createSchemaRepository(@Value("${storage.schema.path}") String storagePath) throws IOException {
        // FIXME:
        Resource file = new FileSystemResource(Paths.get(storagePath, "lmdb"));
        LmdbStoreConfig config = new LmdbStoreConfig();

        log.debug("Initializing persistent schema repository in path '{}'", file.getFile().toPath());

        return new SailRepository(new LmdbStore(file.getFile(), config));
    }



}
