package io.av360.maverick.graph.store.rdf4j.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.av360.maverick.graph.model.security.ApiKeyAuthenticationToken;
import io.av360.maverick.graph.model.security.Authorities;
import io.av360.maverick.graph.store.RepositoryBuilder;
import io.av360.maverick.graph.store.RepositoryType;
import io.av360.maverick.graph.store.rdf.LabeledRepository;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.lmdb.LmdbStore;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j(topic = "graph.repository.config")
@ConfigurationProperties(prefix = "application")
public class DefaultRepositoryBuilder implements RepositoryBuilder {


    @Value("${application.storage.entities.path:#{null}}")
    private String entitiesPath;
    @Value("${application.storage.transactions.path:#{null}}")
    private String transactionsPath;

    @Value("${application.storage.default.path: #{null}}")
    private String schemaPath;

    @Value("${application.storage.default.path: #{null}}")
    private String applicationsPath;
    private final Cache<String, Repository> cache;
    private Map<String, List<String>> storage;
    private String test;
    private Map<String, String> security;


    public DefaultRepositoryBuilder() {

        cache = Caffeine.newBuilder().expireAfterAccess(60, TimeUnit.MINUTES).build();
    }


    /**
     * Initializes the connection to a repository. The repositories are cached
     *
     * @param repositoryType Type of the repository
     * @param authentication Current authentication information
     * @return The repository object
     * @throws IOException If repository cannot be found
     */
    @Override
    public Repository buildRepository(RepositoryType repositoryType, Authentication authentication) throws IOException {
        Assert.notNull(authentication, "Failed to resolve repository due to missing authentication");
        Assert.isTrue(authentication.isAuthenticated(), "Authentication is set to 'false' within a " + authentication.getClass().getSimpleName());


        if (authentication instanceof TestingAuthenticationToken) {
            return this.cache.get(repositoryType.name(), s -> new LabeledRepository("Test:" + repositoryType.name(), new SailRepository(new MemoryStore())));
        }

        if (authentication instanceof ApiKeyAuthenticationToken && Authorities.satisfies(Authorities.READER, authentication.getAuthorities())) {
            log.trace("Resolving default repository with admin authentication and access level: {}.", authentication.getAuthorities());
            return this.resolveRepositoryForDefaultAuthentication(repositoryType, authentication);
        }

        if (authentication instanceof AnonymousAuthenticationToken && Authorities.satisfies(Authorities.READER, authentication.getAuthorities())) {
            log.trace("Resolving default repository with anonymous authentication and access level: {}.", authentication.getAuthorities());
            return this.resolveRepositoryForDefaultAuthentication(repositoryType, authentication);
        }

        throw new IOException(String.format("Cannot resolve repository of type '%s' for authentication of type '%s'", repositoryType, authentication.getClass()));
    }


    protected Repository resolveRepositoryForDefaultAuthentication(RepositoryType repositoryType, Authentication authentication) {

        return switch (repositoryType) {
            case ENTITIES -> this.buildEntityRepository("default");
            case TRANSACTIONS -> this.buildTransactionsRepository("default");
            case APPLICATION -> this.buildApplicationRepository("default");
            case SCHEMA -> this.buildSchemaRepository("default");
        };
    }


    protected Repository buildApplicationRepository(String scope) {
        String key = "applications: " + scope;
        // TODO: check if application has individual schema repo, otherwise we return default
        return this.cache.get(key, s -> new LabeledRepository(key, this.buildDefaultRepository(this.applicationsPath, "applications")));
    }


    protected Repository buildSchemaRepository(String scope) {
        String key = "schema: " + scope;
        // TODO: check if application has individual schema repo, otherwise we return default
        return this.cache.get(key, s -> new LabeledRepository(key, this.buildDefaultRepository(this.schemaPath, "schema")));
    }


    protected Repository buildEntityRepository(String scope) {
        String key = "entities:" + scope;
        return this.cache.get(key, s -> new LabeledRepository(key, this.buildDefaultRepository(this.entitiesPath, "entities")));
    }

    protected Repository buildTransactionsRepository(String scope) {
        String key = "transactions:" + scope;
        return this.cache.get(key, s -> new LabeledRepository(key, this.buildDefaultRepository(this.transactionsPath, "transactions")));
    }


    protected Repository buildDefaultRepository(String basePath, String label) {
        if (!StringUtils.hasLength(basePath)) {
            return this.initializeVolatileRepository(label);
        } else {
            return this.initializePersistentRepository(Paths.get(basePath, label, "lmdb"), label);
        }
    }


    protected Repository initializePersistentRepository(Path path, String label) {
        try {
            Resource file = new FileSystemResource(path);
            LmdbStoreConfig config = new LmdbStoreConfig();

            log.debug("(Store) Initializing persistent {} repository in path '{}'", label, file.getFile().toPath());

            return new SailRepository(new LmdbStore(file.getFile(), config));
        } catch (IOException e) {
            log.error("Failed to initialize persistent {} repository in path '{}'. Falling back to in-memory.", label, path, e);
            return new SailRepository(new MemoryStore());
        }
    }

    protected Repository initializeVolatileRepository(String label) {
        log.debug("(Store) Initializing volatile {} repository for subscription", label);
        return new SailRepository(new MemoryStore());
    }

}
