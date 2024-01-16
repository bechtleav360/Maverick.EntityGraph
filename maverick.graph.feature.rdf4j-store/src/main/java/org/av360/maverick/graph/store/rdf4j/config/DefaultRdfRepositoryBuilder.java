package org.av360.maverick.graph.store.rdf4j.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.model.errors.store.InvalidStoreConfiguration;
import org.av360.maverick.graph.store.FragmentsStore;
import org.av360.maverick.graph.store.RepositoryBuilder;
import org.av360.maverick.graph.store.rdf.LabeledRepository;
import org.av360.maverick.graph.store.rdf4j.repository.util.AbstractRdfRepository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.base.RepositoryWrapper;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.lmdb.LmdbStore;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j(topic = "graph.repo.cfg.builder")
@ConfigurationProperties(prefix = "application")
public class DefaultRdfRepositoryBuilder implements RepositoryBuilder {


    private final Cache<String, LabeledRepository> cache;

    protected MeterRegistry meterRegistry;


    @PreDestroy
    public void shutdownRepositories() {
        if(cache != null) {
            cache.asMap().values().forEach(RepositoryWrapper::shutDown);
        }

    }

    @PostConstruct
    public void init() {

        if(Objects.nonNull(this.meterRegistry)) {
            Gauge.builder("graph.store.repository.cache", cache, Cache::estimatedSize)
                    .tag("metric", "estimatedSize")
                    .register(this.meterRegistry);

            Gauge.builder("graph.store.repository.cache", cache, cache -> cache.stats().evictionCount())
                    .tag("metric", "evictionCount")
                    .register(this.meterRegistry);

            Gauge.builder("graph.store.repository.cache", cache, cache -> cache.stats().loadCount())
                    .tag("metric", "loadCount")
                    .register(this.meterRegistry);

            Gauge.builder("graph.store.repository.cache", cache, cache -> cache.stats().hitCount())
                    .tag("metric", "hitCount")
                    .register(this.meterRegistry);
        }


    }

    @Autowired
    public void setMeterRegistry(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public DefaultRdfRepositoryBuilder() {

        cache = Caffeine.newBuilder()
                .expireAfterAccess(120, TimeUnit.SECONDS)
                .evictionListener((String key, LabeledRepository labeledRepository, RemovalCause cause) -> {
                    log.debug("Repository {} shutting down due to reason: {}", key, cause);
                    if(Objects.nonNull(labeledRepository) && labeledRepository.isInitialized()) {
                        try {
                            labeledRepository.shutDown();
                        } catch (RepositoryException exception) {
                            log.warn("Exception while shutting down for repository {}: {}", key, exception.getMessage());
                        }
                    }
                } )
                .recordStats()
                .build();

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
    public Mono<LabeledRepository> buildRepository(FragmentsStore store, Environment target) {
        if(store instanceof AbstractRdfRepository repository) {
            Validate.isTrue(target.isAuthorized(), "Unauthorized status in repository builder");
            Validate.notNull(target.getRepositoryType(), "Missing repository type in repository builder");
            Validate.notBlank(target.getRepositoryType().toString(), "Empty repository type in repository builder");

            try {
                LabeledRepository labeledRepository = this.buildDefaultRepository(repository, target);
                return this.validateRepository(labeledRepository, repository, target);
            } catch (IOException e) {
                return Mono.error(e);
            }
        } else return Mono.error(new InvalidStoreConfiguration("Store of type %s not supported by for building a RDF repository.".formatted(store.getClass().getSimpleName())));



    }

    @Override
    public Mono<Void> shutdownRepository(FragmentsStore store, Environment environment) {
        if(cache != null) {
            String key = formatRepositoryLabel(environment);
            LabeledRepository repository = cache.getIfPresent(key);
            if(!Objects.isNull(repository)) {
                repository.shutDown();
                cache.invalidate(key);
            }

        }
        return Mono.empty();
    }

    protected Mono<LabeledRepository> validateRepository(@Nullable LabeledRepository repository, FragmentsStore store, Environment environment) throws IOException {
        return Mono.create(sink -> {
            if(!Objects.isNull(repository)) {
                if(! repository.isInitialized() && repository.getConnectionsCount() == 0) {
                    log.warn("Validation error: Repository of type '{}' is not initialized", repository);
                    sink.error(new IOException(String.format("Repository %s not initialized", environment.getRepositoryType())));
                } else {
                    sink.success(repository);
                }
            } else {
                sink.error(new IOException(String.format("Cannot resolve repository of type '%s' for environment '%s'", environment.getRepositoryType(), environment)));
            }
        });
    }

    protected LabeledRepository buildDefaultRepository(AbstractRdfRepository store, Environment target) {

        String key = formatRepositoryLabel(target);

        // FIXME: this should not be bound to an behaviour (but on environment)
        String path = store.getDirectory();

        log.trace("Resolving default repository for environment: {}", target);

        if(Objects.nonNull(this.meterRegistry)) {
            meterRegistry.counter("graph.store.repository", "method", "access", "label", key).increment();
        }


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

            if(Objects.nonNull(this.meterRegistry)) {
                meterRegistry.counter("graph.store.repository", "method", "init", "mode", "persistent", "label", label).increment();
            }


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


        if(Objects.nonNull(this.meterRegistry)) {
            meterRegistry.counter("graph.store.repository", "method", "init", "mode", "volatile", "label", label).increment();
        }


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

    protected String formatRepositoryLabel(Environment environment) {
        StringBuilder label = new StringBuilder(environment.getRepositoryType().toString());
        if(Objects.nonNull(environment.getScope())) label.append("_").append(environment.getScope().label());
        if(StringUtils.hasLength(environment.getStage())) label.append("_").append(environment.getStage());

        return label.toString();
    }

}
