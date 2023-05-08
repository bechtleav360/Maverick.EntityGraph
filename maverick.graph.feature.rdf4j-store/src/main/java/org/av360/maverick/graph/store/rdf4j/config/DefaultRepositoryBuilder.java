package org.av360.maverick.graph.store.rdf4j.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.security.ApiKeyAuthenticationToken;
import org.av360.maverick.graph.store.RepositoryBuilder;
import org.av360.maverick.graph.store.RepositoryType;
import org.av360.maverick.graph.store.behaviours.TripleStore;
import org.av360.maverick.graph.store.rdf.LabeledRepository;
import org.eclipse.rdf4j.http.protocol.UnauthorizedException;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.lmdb.LmdbStore;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

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
     * @param store Store
     * @param authentication Current authentication information
     * @return The repository object
     * @throws IOException If repository cannot be found
     */
    @Override
    public Mono<LabeledRepository> buildRepository(TripleStore store, Authentication authentication) {
        if (Objects.isNull(authentication))
            throw new IllegalArgumentException("Failed to resolve repository due to missing authentication");
        if (!authentication.isAuthenticated())
            throw new UnauthorizedException("Authentication is set to 'false' within the " + authentication.getClass().getSimpleName());

        LabeledRepository repository = null;

        if (authentication instanceof TestingAuthenticationToken) {
            repository = this.buildDefaultRepository(store, "test");
        }
        else if (authentication instanceof ApiKeyAuthenticationToken) {
            repository = this.buildDefaultRepository(store, "default");
        }
        else if (authentication instanceof AnonymousAuthenticationToken) {
            repository = this.buildDefaultRepository(store, "default");
        }

        try {
            return Mono.just(this.validateRepository(repository, store, authentication));
        } catch (IOException e) {
            return Mono.error(e);
        }

    }

    protected LabeledRepository validateRepository(@Nullable LabeledRepository repository, TripleStore store, Authentication authentication) throws IOException {
        if(!Objects.isNull(repository)) {
            if(! repository.isInitialized() && repository.getConnectionsCount() == 0) {
                log.warn("Validation error: Repository of type '{}' is not initialized", repository);
                throw new IOException(String.format("Repository %s not initialized", store.getRepositoryType()));
            }
            return repository;
        } else {
            throw new IOException(String.format("Cannot resolve repository of type '%s' for authentication of type '%s'", store.getRepositoryType(), authentication.getClass()));
        }
    }

    protected LabeledRepository buildDefaultRepository(TripleStore store, String... details) {

        String key = formatRepositoryLabel(store.getRepositoryType(), details);
        String path = store.getDirectory();

        log.trace("Resolving repository of type '{}', label '{}'", store.getRepositoryType(), key);

        if (!StringUtils.hasLength(path)) {
            return getCache().get(key, s -> this.initializeVolatileRepository(key));
        } else {
            Path p = Paths.get(path, "default");

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





    protected LabeledRepository initializePersistentRepository(Path path, String label) {
        try {
            log.debug("Initializing persistent repository in path '{}' for label '{}'", path, label);
            Resource file = new FileSystemResource(path);
            LmdbStoreConfig config = new LmdbStoreConfig();

            config.setTripleIndexes("spoc,ospc,psoc");
           // config.setForceSync(true);


            if (!file.exists() && !file.getFile().mkdirs())
                throw new IOException("Failed to create path: " + file.getFile());

            LabeledRepository labeledRepository = new LabeledRepository(label, new SailRepository(new LmdbStore(file.getFile(), config)));
            labeledRepository.init();
            return labeledRepository;


        } catch (RepositoryException e) {
            log.error("Failed to initialize persistent repository in path '{}'.", path, e);
            throw e;
        } catch (IOException e) {
            log.error("Failed to initialize persistent repository in path '{}'.", path, e);
            return this.initializeVolatileRepository(label);
        }
    }

    protected LabeledRepository initializeVolatileRepository(String label) {
        log.debug("Initializing in-memory repository for label '{}'", label);
        LabeledRepository labeledRepository = new LabeledRepository(label, new SailRepository(new MemoryStore()));
        labeledRepository.init();
        return labeledRepository;
    }

    public Cache<String, LabeledRepository> getCache() {
        return cache;
    }

    protected String formatRepositoryLabel(RepositoryType rt, String... details) {
        StringBuilder label = new StringBuilder(rt.toString());
        for (String appendix : details) {
            label.append("_").append(appendix);
        }
        return label.toString();
    }

}
