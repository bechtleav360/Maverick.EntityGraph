package io.av360.maverick.graph.store.rdf4j.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.av360.maverick.graph.model.security.ApiKeyAuthenticationToken;
import io.av360.maverick.graph.model.util.StreamsLogger;
import io.av360.maverick.graph.store.RepositoryBuilder;
import io.av360.maverick.graph.store.RepositoryType;
import io.av360.maverick.graph.store.rdf.LabeledRepository;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.http.protocol.UnauthorizedException;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
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
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j(topic = "graph.repo.cfg.builder")
@ConfigurationProperties(prefix = "application")
public class DefaultRepositoryBuilder implements RepositoryBuilder {


    @Value("${application.storage.entities.path:#{null}}")
    private String entitiesPath;
    @Value("${application.storage.transactions.path:#{null}}")
    private String transactionsPath;
    @Value("${application.storage.default.path: #{null}}")
    private String defaultPath;

    @Value("${application.storage.default.path: #{null}}")
    private String schemaPath;

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
    public Mono<Repository> buildRepository(RepositoryType repositoryType, Authentication authentication) {
        if (Objects.isNull(authentication))
            return Mono.error(new IllegalArgumentException("Failed to resolve repository due to missing authentication"));
        if (!authentication.isAuthenticated())
            return Mono.error(new UnauthorizedException("Authentication is set to 'false' within the " + authentication.getClass().getSimpleName()));

        if (authentication instanceof TestingAuthenticationToken) {
            return this.getRepository(repositoryType, "test");
        }

        if (authentication instanceof ApiKeyAuthenticationToken) {
            return this.getRepository(repositoryType, "default");
        }

        if (authentication instanceof AnonymousAuthenticationToken) {
            return this.getRepository(repositoryType, "default");
        }

        return Mono.error(new IOException(String.format("Cannot resolve repository of type '%s' for authentication of type '%s'", repositoryType, authentication.getClass())));
    }
    protected Mono<Repository> getRepository(RepositoryType repositoryType, String... details) {
        return Mono.create(sink -> {

            String key = buildRepositoryLabel(repositoryType, details);
            String path = switch (repositoryType) {
                case ENTITIES -> this.entitiesPath;
                case TRANSACTIONS -> this.transactionsPath;
                default -> this.defaultPath;
            };
            log.trace("Resolving repository of type '{}', label '{}'", repositoryType, key);

            if (!StringUtils.hasLength(path)) {
                sink.success(getCache().get(key, s -> new LabeledRepository(key, this.initializeVolatileRepository(repositoryType))));
            } else {
                Path p;
                if(path.equalsIgnoreCase(this.defaultPath)) {
                    p = Paths.get(path, repositoryType.toString(), "lmdb");
                } else {
                    p = Paths.get(path,  "lmdb");
                }
                sink.success(getCache().get(key, s -> new LabeledRepository(key, this.initializePersistentRepository(p, repositoryType))));
            }
        });
                // .doOnSubscribe(StreamsLogger.trace(log, "Resolving repository of type '{}', label '{}'", repositoryType, buildRepositoryLabel(repositoryType, details)));
    }

    private String getPathForRepositoryType(RepositoryType repositoryType) {
        return switch (repositoryType) {
            case ENTITIES -> this.entitiesPath;
            case TRANSACTIONS -> this.transactionsPath;
            case APPLICATION -> this.defaultPath;
            case SCHEMA -> this.schemaPath; 
            default -> this.defaultPath;
        };
    }

    protected Mono<Repository> getRepository3(RepositoryType repositoryType, String... details) {


        String key = buildRepositoryLabel(repositoryType, details);

        Repository repository;
        String basePath = getPathForRepositoryType(repositoryType);
        if (!StringUtils.hasLength(basePath)) {
            repository = getCache().get(key, s -> new LabeledRepository(key, this.initializeVolatileRepository(repositoryType)));
        } else {

            Path p = switch (repositoryType) {
                case ENTITIES -> Paths.get(basePath, "lmdb");
                case TRANSACTIONS -> Paths.get(basePath, "lmdb");
                case APPLICATION -> Paths.get(basePath, "lmbd");
                case SCHEMA -> Paths.get(basePath, "lmbd");
                default -> Paths.get(basePath, repositoryType.toString(), "lmdb");
            };
            repository = getCache().get(key, s -> new LabeledRepository(key, this.initializePersistentRepository(p, repositoryType)));
        }

        return Mono.just(repository)
                .doOnSubscribe(StreamsLogger.trace(log, "Resolving repository of type '{}', label '{}'", repositoryType, key));
    }


    protected Repository initializePersistentRepository(Path path, RepositoryType repositoryType) {
        try {
            log.debug("Initializing persistent repository in path '{}' for label '{}'", path, repositoryType.toString());
            Resource file = new FileSystemResource(path);
            LmdbStoreConfig config = new LmdbStoreConfig();


            if(! file.exists() && ! file.getFile().mkdirs())
                throw new IOException("Failed to create path: "+ file.getFile());

            return new SailRepository(new LmdbStore(file.getFile(), config));



        } catch (RepositoryException | IOException e) {
            log.error("Failed to initialize persistent repository in path '{}'. Falling back to in-memory.", path, e);
            return new SailRepository(new MemoryStore());
        }
    }

    protected Repository initializeVolatileRepository(RepositoryType repositoryType) {
        log.debug("Initializing in-memory repository for label '{}'", repositoryType.toString());
        return new SailRepository(new MemoryStore());
    }

    public Cache<String, Repository> getCache() {
        return cache;
    }

    protected String buildRepositoryLabel(RepositoryType rt, String... details) {
        StringBuilder label = new StringBuilder(rt.toString());
        for (String appendix : details) {
            label.append("_").append(appendix);
        }
        return label.toString();
    }

}
