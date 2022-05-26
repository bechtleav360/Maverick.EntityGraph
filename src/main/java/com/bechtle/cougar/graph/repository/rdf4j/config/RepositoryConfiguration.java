package com.bechtle.cougar.graph.repository.rdf4j.config;

import com.bechtle.cougar.graph.api.security.AdminAuthentication;
import com.bechtle.cougar.graph.features.multitenancy.security.ApplicationAuthentication;
import com.bechtle.cougar.graph.features.multitenancy.domain.model.Application;
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
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j(topic = "cougar.graph.repository.configuration")
@ConfigurationProperties(prefix = "application")
public class RepositoryConfiguration {


    private final String entitiesPath;
    private final String transactionsPath;
    private final Repository schemaRepository;
    private final Repository applicationRepository;
    private final Cache<String, Repository> cache;
    private Map<String, List<String>> storage;
    private String test;
    private Map<String, String> security;

    public enum RepositoryType {
        ENTITIES,
        SCHEMA,
        TRANSACTIONS,
        APPLICATION
    }

    public RepositoryConfiguration(@Value("${application.storage.entities.path:#{null}}") String entitiesPath,
                                   @Value("${application.storage.transactions.path:#{null}}") String transactionsPath,
                                   @Qualifier("schema-storage") Repository schemaRepository,
                                   @Qualifier("application-storage") Repository applicationRepository,
                                   @Value("${application.storage.entities:#{null}}") Map<String, String> storageConfiguration) {

        this.entitiesPath = entitiesPath;
        this.transactionsPath = transactionsPath;
        this.schemaRepository = schemaRepository;
        this.applicationRepository = applicationRepository;


        cache = Caffeine.newBuilder().expireAfterAccess(60, TimeUnit.MINUTES).build();
    }


    /**
     * Initializes the connection to a repository. The repositories are cached
     *
     * @param repositoryType
     * @return
     * @throws IOException
     */
    public Repository getRepository(RepositoryType repositoryType, Authentication authentication) throws IOException {

        if (authentication == null) {
            log.error("No authentication set.");
            throw new IOException("Failed to resolve repository due to missing authentication");
        }

        if (authentication instanceof TestingAuthenticationToken) {
            return this.cache.get(repositoryType.name(), s -> new SailRepository(new MemoryStore()));
        }

        if (authentication instanceof AdminAuthentication) {
            return this.resolveRepositoryForAdminAuthentication(repositoryType, (AdminAuthentication) authentication);
        }

        // FIXME: Dependency into feature.. can we maybe delegate this?
        if (authentication instanceof ApplicationAuthentication) {
            return this.resolveRepositoryForApplicationAuthentication(repositoryType, (ApplicationAuthentication) authentication);
        }

        throw new IOException(String.format("Cannot resolve repository of type '%s' for authentication of type '%s'", repositoryType, authentication.getClass()));
    }

    private Repository resolveRepositoryForApplicationAuthentication(RepositoryType repositoryType, ApplicationAuthentication authentication) throws IOException {
        return switch (repositoryType) {
            case ENTITIES ->  this.getEntityRepository(authentication.getSubscription());
            case TRANSACTIONS -> this.getTransactionsRepository(authentication.getSubscription());
            case SCHEMA ->  this.getSchemaRepository(authentication.getSubscription());
            default -> throw new IOException(String.format("Invalid Repository Type '%s' for subscription context", repositoryType));
        };
    }

    private Repository resolveRepositoryForAdminAuthentication(RepositoryType repositoryType, AdminAuthentication authentication) throws IOException {
        if(repositoryType == RepositoryType.APPLICATION) {
            return this.applicationRepository;
        }

        if(repositoryType == RepositoryType.SCHEMA) {
            return this.schemaRepository;
        }



        if(authentication.getDetails()  != null && authentication.getDetails().getApplication() != null) {
            if(repositoryType == RepositoryType.ENTITIES) {
                return this.getEntityRepository(authentication.getDetails().getApplication().subscription());
            }

            if(repositoryType == RepositoryType.TRANSACTIONS) {
                return this.getTransactionsRepository(authentication.getDetails().getApplication().subscription());
            }
        } else {
            log.warn("No valid application found for admin authentication, we assume this is for tests and return a memory store");
            return this.cache.get(repositoryType.name(), s -> new SailRepository(new MemoryStore()));
        }


        throw new IOException(String.format("Invalid Repository Type '%s' for admin context without a valid subscription key in header.", repositoryType));

    }

    public Mono<Repository> getRepository(RepositoryType repositoryType)  {

        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .switchIfEmpty(Mono.just(new TestingAuthenticationToken("test", "test")))
                .flatMap(authentication -> {
                    try {
                        return Mono.just(this.getRepository(repositoryType, authentication));
                    } catch (IOException e) {
                        return Mono.error(e);
                    }
                });


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
