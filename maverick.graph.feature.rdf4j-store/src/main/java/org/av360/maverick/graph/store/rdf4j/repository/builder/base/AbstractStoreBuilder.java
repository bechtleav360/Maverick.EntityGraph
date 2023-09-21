package org.av360.maverick.graph.store.rdf4j.repository.builder.base;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.store.behaviours.Storable;
import org.av360.maverick.graph.store.rdf4j.extensions.LabeledRepository;
import org.av360.maverick.graph.store.repository.GraphStore;
import org.av360.maverick.graph.store.repository.StoreBuilder;
import org.av360.maverick.graph.store.services.StorageConfigurationService;
import org.eclipse.rdf4j.repository.base.RepositoryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Objects;

@Slf4j(topic = "graph.repo.builder")
public abstract class AbstractStoreBuilder implements StoreBuilder {

    private final Cache<String, LabeledRepository> cache;

    @Autowired
    public void setStorageConfigurationService(StorageConfigurationService storageConfigurationService) {
        this.storageConfigurationService = storageConfigurationService;
    }

    private StorageConfigurationService storageConfigurationService;

    protected AbstractStoreBuilder() {
        cache = Caffeine.newBuilder()
                .recordStats()
                .build();
    }

    public Cache<String, LabeledRepository> getCache() {
        return cache;
    }


    public abstract Mono<Environment> validateInternal(Storable store, Environment environment);

    public abstract Mono<LabeledRepository> buildStoreInternal(String key, Storable store, Environment environment);

    @Override
    public Mono<GraphStore> buildStore(Storable graph, Environment environment) {
        return this.configure(graph, environment)
                .flatMap(env -> this.validate(graph, environment))
                .flatMap(env -> this.buildStoreInternal(this.formatRepositoryLabel(env), graph, env))
                .flatMap(store -> this.checkRepository(store, graph, environment));
    }

    private Mono<Environment> validate(Storable store, Environment environment) {
        Validate.isTrue(environment.isAuthorized(), "Unauthorized status in repository builder");
        Validate.notNull(environment.getRepositoryType(), "Missing repository type in repository builder");
        Validate.notBlank(environment.getRepositoryType().toString(), "Empty repository type in repository builder");


        // TODO: check application path, or set default path for environment
        return this.validateInternal(store, environment);
    }

    private Mono<Environment> configure(Storable store, Environment environment) {
        return storageConfigurationService.loadConfigurationFor(environment);
    }

    private Mono<LabeledRepository> checkRepository(@Nullable LabeledRepository repository, Storable store, Environment environment)  {
        return Mono.create(sink -> {
            if(!Objects.isNull(repository)) {
                if(! repository.isInitialized() && repository.getConnectionsCount() == 0) {
                    log.warn("Validation error: Repository of type '{}' is not initialized", repository);
                    sink.error(new IOException(String.format("Repository %s not initialized", store.getRepositoryType())));
                } else {
                    sink.success(repository);
                }
            } else {
                sink.error(new IOException(String.format("Cannot resolve repository of type '%s' for environment '%s'", store.getRepositoryType(), environment)));
            }
        });
    }

    @Override
    public Mono<Void> shutdownStore(Storable store, Environment environment) {
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


    protected String formatRepositoryLabel(Environment environment) {
        StringBuilder label = new StringBuilder(environment.getRepositoryType().toString());
        if(Objects.nonNull(environment.getScope())) label.append("_").append(environment.getScope().label());
        if(StringUtils.hasLength(environment.getStage())) label.append("_").append(environment.getStage());

        return label.toString();
    }

    @PreDestroy
    public void shutdownRepositories() {
        if(cache != null) {
            cache.asMap().values().forEach(RepositoryWrapper::shutDown);
        }
    }
}
