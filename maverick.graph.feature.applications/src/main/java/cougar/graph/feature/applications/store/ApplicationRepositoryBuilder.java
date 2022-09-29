package cougar.graph.feature.applications.store;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import cougar.graph.feature.applications.security.ApplicationAuthenticationToken;
import cougar.graph.store.rdf.LabeledRepository;
import cougar.graph.model.security.ApiKeyAuthenticationToken;
import cougar.graph.model.security.Authorities;
import cougar.graph.store.RepositoryBuilder;
import cougar.graph.store.RepositoryType;
import cougar.graph.feature.applications.domain.model.Application;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.lmdb.LmdbStore;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Replaces the @see DefaultRepositoryBuilder
 *
 */
@Component
@Slf4j(topic = "graph.repository.config")
@ConfigurationProperties(prefix = "application")
@Primary
public class ApplicationRepositoryBuilder implements RepositoryBuilder {


    private final String entitiesPath;
    private final String transactionsPath;
    private final String schemaPath;
    private final String applicationsPath;
    private final Cache<String, Repository> cache;
    private Map<String, List<String>> storage;
    private String test;
    private Map<String, String> security;


    public ApplicationRepositoryBuilder(@Value("${application.storage.entities.path:#{null}}") String entitiesPath,
                                        @Value("${application.storage.transactions.path:#{null}}") String transactionsPath,
                                        @Value("${application.storage.default.path: #{null}}") String schemaPath,
                                        @Value("${application.storage.default.path: #{null}}") String applicationsPath,
                                        @Value("${application.storage.entities:#{null}}") Map<String, String> storageConfiguration) throws IOException {

        this.entitiesPath = entitiesPath;
        this.transactionsPath = transactionsPath;
        this.schemaPath = schemaPath;
        this.applicationsPath = applicationsPath;

        cache = Caffeine.newBuilder().expireAfterAccess(60, TimeUnit.MINUTES).build();
    }




    /**
     * Initializes the connection to a repository. The repositories are cached.
     * <p>
     * We assert a valid and positive authentication at this point.
     *
     * @param repositoryType    the type, e.g. for schema or entities
     * @return the repository
     * @throws IOException if repository cannot be resolved
     */
    @Override
    public Repository buildRepository(RepositoryType repositoryType, Authentication authentication) throws IOException {
        Assert.notNull(authentication, "Failed to resolve repository due to missing authentication");
        Assert.isTrue(authentication.isAuthenticated(), "Authentication is set to 'false' within a "+authentication.getClass().getSimpleName());


        if (authentication instanceof TestingAuthenticationToken) {
            return this.cache.get(repositoryType.name(), s -> new LabeledRepository("test:" + repositoryType.name(), new SailRepository(new MemoryStore())));
        }

        if (authentication instanceof ApiKeyAuthenticationToken && authentication.getAuthorities().contains(Authorities.ADMIN)) {
            return this.resolveRepositoryForAdminAuthentication(repositoryType, (ApiKeyAuthenticationToken) authentication);
        }

        if (authentication instanceof ApplicationAuthenticationToken && authentication.getAuthorities().contains(Authorities.USER)) {
            return this.resolveRepositoryForApplicationAuthentication(repositoryType, (ApplicationAuthenticationToken) authentication);
        }

        throw new IOException(String.format("Cannot resolve repository of type '%s' for authentication of type '%s'", repositoryType, authentication.getClass()));
    }

    private Repository resolveRepositoryForApplicationAuthentication(RepositoryType repositoryType, ApplicationAuthenticationToken authentication) throws IOException {
        Assert.isTrue(authentication.getAuthorities().contains(Authorities.USER), "Missing authorization: " + Authorities.USER.getAuthority());

        log.trace("(Store) Resolving repository with application authentication.");
        return switch (repositoryType) {
            case ENTITIES -> this.getEntityRepository(authentication.getSubscription());
            case TRANSACTIONS -> this.getTransactionsRepository(authentication.getSubscription());
            case SCHEMA -> this.getSchemaRepository(authentication.getSubscription());
            // application is left out.. we cannot access the applications details with a user authorization only
            default ->
                    throw new IOException(String.format("Invalid Repository Type '%s' for application context", repositoryType));
        };
    }

    private Repository resolveRepositoryForAdminAuthentication(RepositoryType repositoryType, ApiKeyAuthenticationToken authentication) throws IOException {
        Assert.isTrue(authentication.getAuthorities().contains(Authorities.ADMIN), "Missing authorization: " + Authorities.ADMIN.getAuthority());

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
        return this.cache.get(key, s -> new LabeledRepository(key, this.buildDefaultRepository(this.applicationsPath, "applications")));
    }


    private Repository getSchemaRepository(@Nullable Application application) {
        if(application == null) {
            String key = "schema: default";
            return this.cache.get(key, s -> new LabeledRepository(key, this.buildDefaultRepository(this.schemaPath, "schema")));
        } else {
            log.warn("Application-scoped schema repositories are not supported yet");
            String key = "schema: default";
            return this.cache.get(key, s -> new LabeledRepository(key, this.buildDefaultRepository(this.schemaPath, "schema")));
        }

        // TODO: check if application has individual schema repo, otherwise we return default

    }


    private Repository getEntityRepository(@Nullable Application application) throws IOException {
        if(application == null) {
            String key = "entities: default";
            return this.cache.get(key, s -> new LabeledRepository(key, this.buildDefaultRepository(this.applicationsPath, "entities")));
        } else {
            String key = "entities: " + application.key();
            return this.cache.get(key, s -> new LabeledRepository(key, this.buildApplicationsRepository(application, "entities", this.entitiesPath)));
        }

    }

    private Repository getTransactionsRepository(@Nullable Application application) throws IOException {
        if(application == null) {
            String key = "transactions: default";
            return this.cache.get(key, s -> new LabeledRepository(key, this.buildDefaultRepository(this.transactionsPath, "entities")));
        } else {
            String key = "transactions: " + application.key();
            return this.cache.get(key, s -> new LabeledRepository(key, this.buildApplicationsRepository(application, "transactions", this.transactionsPath)));
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
