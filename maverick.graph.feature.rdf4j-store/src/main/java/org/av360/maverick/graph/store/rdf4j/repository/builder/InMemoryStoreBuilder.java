package org.av360.maverick.graph.store.rdf4j.repository.builder;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.store.behaviours.Storable;
import org.av360.maverick.graph.store.rdf4j.extensions.LabeledRepository;
import org.av360.maverick.graph.store.rdf4j.repository.builder.base.MonitoredStoreBuilder;
import org.av360.maverick.graph.store.repository.StoreBuilder;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Service
@Slf4j(topic = "graph.repo.builder")
public class InMemoryStoreBuilder extends MonitoredStoreBuilder implements StoreBuilder {
    @Override
    public boolean canBuild(Environment environment) {
        boolean result = ! environment.isFlagged(Environment.RepositoryFlag.PERSISTENT)
                && ! environment.isFlagged(Environment.RepositoryFlag.REMOTE);

        return result;
    }

    @Override
    public Mono<Environment> validateInternal(Storable store, Environment environment) {
        return Mono.just(environment);
    }

    @Override
    public Mono<LabeledRepository> buildStoreInternal(String key, Storable store, Environment environment) {

        LabeledRepository repository = getCache().get(key, s -> {
            log.debug("Initializing in-memory repository for label '{}'", key);


            if (Objects.nonNull(this.meterRegistry)) {
                meterRegistry.counter("graph.store.repository", "method", "init", "mode", "volatile", "label", key).increment();
            }
            LabeledRepository labeledRepository = new LabeledRepository(key, new SailRepository(new MemoryStore()));
            labeledRepository.init();
            return labeledRepository;
        });

        return Mono.just(repository);
    }
}
