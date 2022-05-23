package com.bechtle.eagl.graph.repository.rdf4j.config;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.lmdb.LmdbStore;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.springframework.beans.factory.annotation.Value;
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
@Profile({"prod", "stage", "it", "persistent"})
@Slf4j
public class DefaultPersistentRepositoryConfiguration {

    // FIXME: Should not be configured through profiles, but as a flag in the applicaation2
    @Bean("schema-storage")
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public Repository createSchemaRepository(@Value("${application.storage.default.path}") String storagePath) throws IOException {
        // FIXME:
        Resource file = new FileSystemResource(Paths.get(storagePath, "schema", "lmdb"));
        LmdbStoreConfig config = new LmdbStoreConfig();

        log.debug("Initializing persistent schema repository in path '{}'", file.getFile().toPath());

        return new SailRepository(new LmdbStore(file.getFile(), config));
    }

    @Bean("application-storage")
    public Repository createApplicationRepository(@Value("${application.storage.default.path}") String storagePath) throws IOException {
        Resource file = new FileSystemResource(Paths.get(storagePath, "subscriptions", "lmdb"));
        LmdbStoreConfig config = new LmdbStoreConfig();

        log.debug("Initializing persistent application repository in path '{}'", file.getFile().toPath());


        return new SailRepository(new LmdbStore(file.getFile(), config));
    }
}
