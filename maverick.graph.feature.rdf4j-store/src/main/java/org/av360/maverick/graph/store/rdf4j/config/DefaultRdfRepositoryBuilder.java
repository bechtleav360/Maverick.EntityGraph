package org.av360.maverick.graph.store.rdf4j.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
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
import org.eclipse.rdf4j.repository.RepositoryLockedException;
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
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

@Component
@Slf4j(topic = "graph.repo.cfg.builder")
@ConfigurationProperties(prefix = "application")
public class DefaultRdfRepositoryBuilder implements RepositoryBuilder {


    private RepositoryCache cache;
    protected MeterRegistry meterRegistry;

    @PreDestroy
    public void shutdownRepositories() {
        cache.shutdown();
    }


    @Autowired
    public void setMeterRegistry(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }


    @PostConstruct
    private void registerCacheGauges() {
        this.cache.init();

        if (Objects.nonNull(this.meterRegistry)) {
            Gauge.builder("graph.store.repository.cache_size", this.cache.items(), Collection::size)
                    .register(this.meterRegistry);

        }
    }


    private synchronized Mono<LabeledRepository> getCached(String label, Function<String, LabeledRepository> mappingFunction) {
        if (this.cache.contains(label)) {
            return Mono.just(this.cache.get(label));
        } else {
            return Mono.just(mappingFunction.apply(label)).cache(Duration.of(10, ChronoUnit.SECONDS));
        }
    }


    public DefaultRdfRepositoryBuilder() {
        this.cache = new RepositoryCache();
    }


    /**
     * Initializes the connection to a repository. The repositories are cached
     *
     * @param store          Store
     * @param authentication Current authentication information
     * @return The repository object
     * @throws IOException If repository cannot be found
     */
    @Override
    public Mono<LabeledRepository> getRepository(FragmentsStore store, Environment target) {
        if (store instanceof AbstractRdfRepository rdfStore) {
            Validate.isTrue(target.isAuthorized(), "Unauthorized status in repository builder");
            Validate.notNull(target.getRepositoryType(), "Missing repository type in repository builder");
            Validate.notBlank(target.getRepositoryType().toString(), "Empty repository type in repository builder");

            return this.buildRepository(rdfStore, target)
                    .flatMap(repository -> this.validateRepository(repository, rdfStore, target));
        } else
            return Mono.error(new InvalidStoreConfiguration("Store of type %s not supported by for building a RDF repository.".formatted(store.getClass().getSimpleName())));

    }

    @Override
    public Mono<Void> shutdownRepository(FragmentsStore store, Environment environment) {
        String key = formatRepositoryLabel(environment);
        this.cache.shutdown(key);
        return Mono.empty();
    }

    protected Mono<LabeledRepository> validateRepository(@Nullable LabeledRepository repository, FragmentsStore store, Environment environment) {
        return Mono.create(sink -> {
            if (!Objects.isNull(repository)) {
                if (!repository.isInitialized() && repository.getConnectionsCount() == 0) {
                    repository.init();
                    sink.success(repository);
                } else {
                    sink.success(repository);
                }
            } else {
                sink.error(new IOException(String.format("Cannot resolve repository of type '%s' for environment '%s'", environment.getRepositoryType(), environment)));
            }
        });
    }

    protected Mono<LabeledRepository> buildRepository(AbstractRdfRepository store, Environment environment) {
        if (!environment.hasConfiguration(Environment.RepositoryConfigurationKey.FLAG_PERSISTENT)) {
            log.warn("Repository configuration for persistence not present, default to false");
            environment.setConfiguration(Environment.RepositoryConfigurationKey.FLAG_PERSISTENT, false);
        }

        if (!environment.hasConfiguration(Environment.RepositoryConfigurationKey.FLAG_PUBLIC)) {
            log.warn("Repository configuration for persistence not present, default to false");
            environment.setConfiguration(Environment.RepositoryConfigurationKey.FLAG_PUBLIC, false);
        }


        if (Boolean.parseBoolean(environment.getConfiguration(Environment.RepositoryConfigurationKey.FLAG_PERSISTENT).get())) {
            // TODO: either default path is set, or application has a configuration set for the path. For now we expect the default path
            Validate.notBlank(store.getDirectory(), "No default storage directory defined for persistent application");
        }


        log.trace("Resolving repository for environment: {}", environment);

        String label = formatRepositoryLabel(environment);
        if (Objects.nonNull(this.meterRegistry)) {
            meterRegistry.counter("graph.store.repository", "method", "access", "label", label).increment();
        }


        if (environment.getConfiguration(Environment.RepositoryConfigurationKey.FLAG_PERSISTENT).map(Boolean::parseBoolean).orElse(false)) {
            Path path;
            if (environment.hasConfiguration(Environment.RepositoryConfigurationKey.KEY)) {
                path = Paths.get(store.getDirectory(), environment.getConfiguration(Environment.RepositoryConfigurationKey.KEY).get());
            } else {
                path = Paths.get(store.getDirectory());
            }

            return getCached(label, s -> initializePersistentRepository(path, label));
        } else {
            return getCached(label, s -> initializeVolatileRepository(label));
        }
    }


    protected synchronized LabeledRepository initializePersistentRepository(Path path, String label) {
        try {
            if(this.cache.contains(label)) {
                return this.cache.get(label);
            }

            log.debug("Initializing persistent repository in path '{}' for label '{}'", path, label);

            Resource file = new FileSystemResource(path);
            LmdbStoreConfig config = new LmdbStoreConfig();

            config.setTripleIndexes("spoc,ospc,psoc");
            config.setForceSync(false);
            


            if (!file.exists() && !file.getFile().mkdirs())
                throw new IOException("Failed to create path: " + file.getFile());

            try {

                LabeledRepository labeledRepository = new LabeledRepository(label, new SailRepository(new LmdbStore(file.getFile(), config)));
                labeledRepository.init();
                this.registerMetrics(label, labeledRepository);

                this.cache.register(label, labeledRepository);
                
                return this.cache.get(label);
            } catch (RepositoryLockedException lockedException) {
                log.warn("Failed to init persistent repository, it is locked");
                if(this.cache.contains(label)) {
                    return this.cache.get(label);
                }
                throw lockedException;
            }






        } catch (RepositoryException e) {
            log.error("Failed to initialize persistent repository in path '{}'.", path, e);
            throw e;
        } catch (IOException e) {
            log.error("Failed to initialize persistent repository in path '{}'.", path, e);
            return this.initializeVolatileRepository(label);
        }
    }


    private void registerMetrics(String label, LabeledRepository labeledRepository) {
        if (Objects.nonNull(this.meterRegistry)) {
            meterRegistry.counter("graph.store.repository", "method", "init", "mode", "persistent", "label", label).increment();
            meterRegistry.gauge("graph.store.repository.connections", Tags.of("label", label), labeledRepository, LabeledRepository::getConnectionsCount);
        }
    }

    protected synchronized LabeledRepository initializeVolatileRepository(String label) {
        log.debug("Initializing in-memory repository for label '{}'", label);


        LabeledRepository labeledRepository = new LabeledRepository(label, new SailRepository(new MemoryStore()));
        this.cache.register(label, labeledRepository);

        labeledRepository.init();

        this.registerMetrics(label, labeledRepository);

        return this.cache.get(label);
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
        if (Objects.nonNull(environment.getScope())) label.append("_").append(environment.getScope().label());
        if (StringUtils.hasLength(environment.getStage())) label.append("_").append(environment.getStage());

        return label.toString();
    }

}
