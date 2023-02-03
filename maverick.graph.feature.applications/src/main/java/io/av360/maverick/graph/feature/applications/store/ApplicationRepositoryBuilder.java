package io.av360.maverick.graph.feature.applications.store;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.av360.maverick.graph.feature.applications.domain.model.Application;
import io.av360.maverick.graph.feature.applications.security.ApplicationAuthenticationToken;
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
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * Replaces the @see DefaultRepositoryBuilder
 */
@Component
@Slf4j(topic = "graph.repository.config")
@Primary
public class ApplicationRepositoryBuilder implements RepositoryBuilder {


    @Value("${application.storage.entities.path:#{null}}")
    private String entitiesPath;
    @Value("${application.storage.transactions.path:#{null}}")
    private String transactionsPath;
    @Value("${application.storage.default.path: #{null}}")
    private String schemaPath;
    @Value("${application.storage.default.path: #{null}}")
    private String applicationsPath;
    private final Cache<String, Repository> repositoryCache;


    public ApplicationRepositoryBuilder() {
        repositoryCache = Caffeine.newBuilder().expireAfterAccess(60, TimeUnit.MINUTES).build();
    }


    /**
     * Initializes the connection to a repository. The connections are cached.
     * <p>
     * We assert a valid and positive authentication at this point.
     *
     * @param repositoryType the type, e.g. for schema or entities
     * @return the repository
     * @throws IOException if repository cannot be resolved
     */
    @Override
    public Repository buildRepository(RepositoryType repositoryType, Authentication authentication) throws IOException {
        Assert.notNull(authentication, "Failed to resolve repository due to missing authentication");
        Assert.isTrue(authentication.isAuthenticated(), "Authentication is set to 'false' within a " + authentication.getClass().getSimpleName());


        if (authentication instanceof TestingAuthenticationToken) {
            return this.repositoryCache.get(repositoryType.name(), s -> new LabeledRepository("test:" + repositoryType.name(), new SailRepository(new MemoryStore())));
        }

        if (authentication instanceof ApplicationAuthenticationToken && Authorities.satisfies(Authorities.READER, authentication.getAuthorities())) {
            return this.resolveRepositoryForApplicationAuthentication(repositoryType, (ApplicationAuthenticationToken) authentication);
        }

        if (authentication instanceof ApiKeyAuthenticationToken && Authorities.satisfies(Authorities.SYSTEM, authentication.getAuthorities())) {
            return this.resolveRepositoryForDefaultAuthentication(repositoryType, authentication);
        }

        if (authentication instanceof AnonymousAuthenticationToken && Authorities.satisfies(Authorities.READER, authentication.getAuthorities())) {
            return this.resolveRepositoryForDefaultAuthentication(repositoryType, authentication);
        }


        throw new IOException(String.format("Cannot resolve repository of type '%s' for authentication of type '%s'", repositoryType, authentication.getClass()));
    }


    private Repository resolveRepositoryForApplicationAuthentication(RepositoryType repositoryType, ApplicationAuthenticationToken authentication) throws IOException {
        Assert.isTrue(Authorities.satisfies(Authorities.READER, authentication.getAuthorities()), "Missing authorization: " + Authorities.READER.getAuthority());

        log.trace("Resolving repository with application authentication.");
        return switch (repositoryType) {
            case ENTITIES -> this.getEntityRepository(authentication.getSubscription());
            case TRANSACTIONS -> this.getTransactionsRepository(authentication.getSubscription());
            case SCHEMA -> this.getSchemaRepository(authentication.getSubscription());
            // application is left out.. we cannot access the applications details with a user authorization only
            default ->
                    throw new IOException(String.format("Invalid Repository Type '%s' for application context", repositoryType));
        };
    }

    private Repository resolveRepositoryForDefaultAuthentication(RepositoryType repositoryType, Authentication authentication) {
        if (Authorities.satisfies(Authorities.SYSTEM, authentication.getAuthorities()) && authentication instanceof ApplicationAuthenticationToken) {
                log.trace("Resolving repository with admin authentication and additional subscription key.");
                Application subscription = ((ApplicationAuthenticationToken) authentication).getSubscription();
                return switch (repositoryType) {
                    case ENTITIES -> this.getEntityRepository(subscription);
                    case TRANSACTIONS -> this.getTransactionsRepository(subscription);
                    case APPLICATION -> this.getApplicationRepository();
                    case SCHEMA -> this.getSchemaRepository(subscription);
                };
        } else {
            if(authentication instanceof AnonymousAuthenticationToken) {
                log.trace("Resolving repository with anonymous authentication with read access. Entities and transactions are default (probably in-memory) .");
            } else {
                log.trace("Resolving repository with admin token without additional subscription key). Entities and transactions are default (probably in-memory) .");
            }

            return switch (repositoryType) {
                case ENTITIES -> this.getEntityRepository(null);
                case TRANSACTIONS -> this.getTransactionsRepository(null);
                case APPLICATION -> this.getApplicationRepository();
                case SCHEMA -> this.getSchemaRepository(null);
            };
        }
    }




    @Deprecated
    private Repository resolveRepositoryForSystemAuthentication(RepositoryType repositoryType, ApiKeyAuthenticationToken authentication) throws IOException {
        Assert.isTrue(Authorities.satisfies(Authorities.SYSTEM, authentication.getAuthorities()), "Missing authorization: " + Authorities.SYSTEM.getAuthority());

        if (authentication instanceof ApplicationAuthenticationToken) {
            log.trace("Resolving repository with admin authentication and additional subscription key.");
            Application subscription = ((ApplicationAuthenticationToken) authentication).getSubscription();
            return switch (repositoryType) {
                case ENTITIES -> this.getEntityRepository(subscription);
                case TRANSACTIONS -> this.getTransactionsRepository(subscription);
                case APPLICATION -> this.getApplicationRepository();
                case SCHEMA -> this.getSchemaRepository(subscription);
            };

        } else {
            log.trace("Resolving repository with admin authentication without additional subscription key. Entities and transactions are in-memory only.");
            return switch (repositoryType) {
                case ENTITIES -> this.getEntityRepository(null);
                case TRANSACTIONS -> this.getTransactionsRepository(null);
                case APPLICATION -> this.getApplicationRepository();
                case SCHEMA -> this.getSchemaRepository(null);
            };
        }
    }


    private Repository getApplicationRepository() {
        String key = "applications: default";
        return this.repositoryCache.get(key, s -> new LabeledRepository(key, this.buildDefaultRepository(this.applicationsPath, "applications")));
    }


    private Repository getSchemaRepository(@Nullable Application application) {
        if (application == null) {
            String key = "schema: default";
            return this.repositoryCache.get(key, s -> new LabeledRepository(key, this.buildDefaultRepository(this.schemaPath, "schema")));
        } else {
            log.warn("Application-scoped schema repositories are not supported yet");
            String key = "schema: default";
            return this.repositoryCache.get(key, s -> new LabeledRepository(key, this.buildDefaultRepository(this.schemaPath, "schema")));
        }

        // TODO: check if application has individual schema repo, otherwise we return default

    }


    private Repository getEntityRepository(@Nullable Application application) {
        if (application == null) {
            String key = "entities: default";
            return this.repositoryCache.get(key, s -> new LabeledRepository(key, this.buildDefaultRepository(this.applicationsPath, "entities")));
        } else {
            String key = "entities: " + application.key();
            return this.repositoryCache.get(key, s -> new LabeledRepository(key, this.buildApplicationsRepository(application, "entities", this.entitiesPath)));
        }

    }

    private Repository getTransactionsRepository(@Nullable Application application) {
        if (application == null) {
            String key = "transactions: default";
            return this.repositoryCache.get(key, s -> new LabeledRepository(key, this.buildDefaultRepository(this.transactionsPath, "entities")));
        } else {
            String key = "transactions: " + application.key();
            return this.repositoryCache.get(key, s -> new LabeledRepository(key, this.buildApplicationsRepository(application, "transactions", this.transactionsPath)));
        }
    }


    private Repository buildDefaultRepository(String basePath, String label) {
        if (!StringUtils.hasLength(basePath)) {
            log.debug("(Store) Initializing volatile {} repository for application", label);
            return new SailRepository(new MemoryStore());
        } else {
            return this.initializePersistentRepository(Paths.get(basePath, label, "lmdb"), label);
        }
    }


    private Repository buildApplicationsRepository(Application subscription, String label, String basePath) {
        if (!subscription.persistent() || !StringUtils.hasLength(basePath)) {
            log.debug("(Store) Initializing volatile {} repository for application '{}' [{}]", label, subscription.label(), subscription.key());
            return new SailRepository(new MemoryStore());
        } else {
            Path path = Paths.get(basePath, subscription.key(), label, "lmdb");
            return this.initializePersistentRepository(path, label);
        }
    }

    private Repository initializePersistentRepository(Path path, String label) {
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

}
