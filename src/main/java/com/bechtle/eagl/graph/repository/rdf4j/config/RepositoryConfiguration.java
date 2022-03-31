package com.bechtle.eagl.graph.repository.rdf4j.config;

import com.bechtle.eagl.graph.api.security.AdminAuthentication;
import com.bechtle.eagl.graph.features.multitenancy.security.ApplicationAuthentication;
import com.bechtle.eagl.graph.features.multitenancy.domain.model.Application;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.lmdb.LmdbStore;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class RepositoryConfiguration {


    private final String entitiesPath;
    private final String transactionsPath;
    private final Repository schemaRepository;
    private final Repository subscriptionsRepository;
    private final Cache<String, Repository> cache;

    public enum RepositoryType {
        ENTITIES,
        SCHEMA,
        TRANSACTIONS,
        SUBSCRIPTIONS
    }

    public RepositoryConfiguration(@Value("${application.storage.entities.path:#{null}}") String entitiesPath,
                                   @Value("${application.storage.transactions.path:#{null}}") String transactionsPath,
                                   @Qualifier("schema-storage") Repository schemaRepository,
                                   @Qualifier("subscriptions-storage") Repository subscriptionsRepository) {
        this.entitiesPath = entitiesPath;
        this.transactionsPath = transactionsPath;
        this.schemaRepository = schemaRepository;
        this.subscriptionsRepository = subscriptionsRepository;


        cache = Caffeine.newBuilder().expireAfterAccess(60, TimeUnit.MINUTES).build();
    }


    /**
     * Initializes the connection to a repository. The repository are cached
     *
     * @param repositoryType
     * @return
     * @throws IOException
     */
    public Repository getRepository(RepositoryType repositoryType) throws IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {

            return this.cache.get("none", s -> {
                log.warn("(Store) No authentication set, using in memory store for type {}", repositoryType);
                return new SailRepository(new MemoryStore());
            });
        }

        if (authentication instanceof AdminAuthentication) {
            return switch (repositoryType) {
                case SUBSCRIPTIONS -> this.subscriptionsRepository;
                case SCHEMA -> this.schemaRepository;
                default -> throw new IOException(String.format("Invalid Repository Type '%s' for admin context", repositoryType));
            };
        }

        // FIXME: Dependency into feature.. can we maybe delegate this?
        if (authentication instanceof ApplicationAuthentication) {
            return switch (repositoryType) {
                case ENTITIES -> this.getEntityRepository(((ApplicationAuthentication) authentication).getSubscription());
                case TRANSACTIONS -> this.getTransactionsRepository(((ApplicationAuthentication) authentication).getSubscription());
                case SCHEMA -> this.getSchemaRepository(((ApplicationAuthentication) authentication).getSubscription());
                default -> throw new IOException(String.format("Invalid Repository Type '%s' for subscription context", repositoryType));
            };

        }

        throw new IOException(String.format("Cannot resolve repository of type '%s' for authentication of type '%s'", repositoryType, authentication.getClass()));

    }

    private Repository getDefaultRepository(Application subscription, String label, String basePath) {
        if (!subscription.persistent() || !StringUtils.hasLength(basePath)) {
            log.debug("(Store) Initializing volatile {} repository for subscription '{}' [{}]", label, subscription.label(), subscription.key());
            return new SailRepository(new MemoryStore());
        } else {
            Resource file = new FileSystemResource(Paths.get(basePath, subscription.key(), label, "lmdb"));
            Assert.notNull(file, "Invalid path to repository: " + file);
            try {
                LmdbStoreConfig config = new LmdbStoreConfig();

                log.debug("(Store) Initializing persistent {} repository in path '{}'", label, file.getFile().toPath());


                return new SailRepository(new LmdbStore(file.getFile(), config));
            } catch (IOException e) {
                log.error("Failed to initialize persistent {}  repository in path '{}'. Falling back to in-memory.", label, file, e);
                return new SailRepository(new MemoryStore());
            }
        }
    }

    @Cacheable
    private Repository getSchemaRepository(Application subscription) {
        return this.schemaRepository; 
    }


    @Cacheable
    public Repository getEntityRepository(Application subscription) throws IOException {
        return this.cache.get("entities:" + subscription.key(), s -> this.getDefaultRepository(subscription, "entities", this.entitiesPath));
    }

    @Cacheable
    public Repository getTransactionsRepository(Application subscription) throws IOException {
        return this.cache.get("transactions:" + subscription.key(), s -> this.getDefaultRepository(subscription, "transactions", this.transactionsPath));
    }


}
