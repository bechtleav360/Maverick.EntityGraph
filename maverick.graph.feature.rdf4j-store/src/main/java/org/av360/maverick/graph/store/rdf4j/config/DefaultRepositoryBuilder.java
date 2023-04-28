package org.av360.maverick.graph.store.rdf4j.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.security.ApiKeyAuthenticationToken;
import org.av360.maverick.graph.store.RepositoryBuilder;
import org.av360.maverick.graph.store.RepositoryType;
import org.av360.maverick.graph.store.rdf.LabeledRepository;
import org.eclipse.rdf4j.http.protocol.UnauthorizedException;
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

import javax.annotation.Nullable;
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

    private final Cache<String, LabeledRepository> cache;
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
    public LabeledRepository buildRepository(RepositoryType repositoryType, Authentication authentication) throws IOException {
        if (Objects.isNull(authentication))
            throw new IllegalArgumentException("Failed to resolve repository due to missing authentication");
        if (!authentication.isAuthenticated())
            throw new UnauthorizedException("Authentication is set to 'false' within the " + authentication.getClass().getSimpleName());

        LabeledRepository repository = null;

        if (authentication instanceof TestingAuthenticationToken) {
            repository = this.getRepository(repositoryType, "test");
        }
        else if (authentication instanceof ApiKeyAuthenticationToken) {
            repository = this.getRepository(repositoryType, "default");
        }
        else if (authentication instanceof AnonymousAuthenticationToken) {
            repository = this.getRepository(repositoryType, "default");
        }

        return validateRepository(repository, repositoryType, authentication);

    }

    protected LabeledRepository validateRepository(@Nullable LabeledRepository repository, RepositoryType repositoryType, Authentication authentication) throws IOException {
        if(!Objects.isNull(repository)) {
            if(! repository.isInitialized()) {
                repository.init();
            }
            return repository;
        } else {
            throw new IOException(String.format("Cannot resolve repository of type '%s' for authentication of type '%s'", repositoryType, authentication.getClass()));
        }
    }

    protected LabeledRepository getRepository(RepositoryType repositoryType, String... details) {

        String key = buildRepositoryLabel(repositoryType, details);
        String path = switch (repositoryType) {
            case ENTITIES -> this.entitiesPath;
            case TRANSACTIONS -> this.transactionsPath;
            default -> this.defaultPath;
        };
        log.trace("Resolving repository of type '{}', label '{}'", repositoryType, key);

        if (!StringUtils.hasLength(path)) {
            return getCache().get(key, s -> this.initializeVolatileRepository(key));
        } else {
            Path p;
            if (path.equalsIgnoreCase(this.defaultPath)) {
                p = Paths.get(path, repositoryType.toString(), "lmdb");
            } else {
                p = Paths.get(path, "lmdb");
            }

            LabeledRepository repository = getCache().getIfPresent(key);

            if (!Objects.isNull(repository) && repository.isInitialized()) {
                return repository;
            } else if (!Objects.isNull(repository) && !repository.isInitialized()) {
                log.warn("Repository '{}' was cached and built, but not initialized.", key);
                repository.init();
                return repository;
            } else {
                return getCache().get(key, s -> this.initializePersistentRepository(p, key));
            }
        }
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



    protected LabeledRepository initializePersistentRepository(Path path, String label) {
        try {
            log.debug("Initializing persistent repository in path '{}' for label '{}'", path, label);
            Resource file = new FileSystemResource(path);
            LmdbStoreConfig config = new LmdbStoreConfig();


            if (!file.exists() && !file.getFile().mkdirs())
                throw new IOException("Failed to create path: " + file.getFile());

            return new LabeledRepository(label, new SailRepository(new LmdbStore(file.getFile(), config)));


        } catch (RepositoryException | IOException e) {
            log.error("Failed to initialize persistent repository in path '{}'. Falling back to in-memory.", path, e);
            return this.initializeVolatileRepository(label);
        }
    }

    protected LabeledRepository initializeVolatileRepository(String label) {
        log.debug("Initializing in-memory repository for label '{}'", label);
        return new LabeledRepository(label, new SailRepository(new MemoryStore()));
    }

    public Cache<String, LabeledRepository> getCache() {
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
