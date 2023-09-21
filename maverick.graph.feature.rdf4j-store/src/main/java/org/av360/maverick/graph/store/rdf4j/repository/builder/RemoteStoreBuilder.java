package org.av360.maverick.graph.store.rdf4j.repository.builder;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.store.behaviours.Storable;
import org.av360.maverick.graph.store.rdf4j.extensions.LabeledRepository;
import org.av360.maverick.graph.store.rdf4j.repository.builder.base.MonitoredStoreBuilder;
import org.av360.maverick.graph.store.repository.StoreBuilder;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Service
@Slf4j(topic = "graph.repo.builder.remote")
public class RemoteStoreBuilder extends MonitoredStoreBuilder implements StoreBuilder {
    @Override
    public boolean canBuild(Environment environment) {
        boolean result = environment.isFlagged(Environment.RepositoryFlag.REMOTE);

        return result;
    }

    @Override
    public Mono<Environment> validateInternal(Storable store, Environment environment) {
        // TODO: required keys: label of remote instance (which resolves to host), name of graph

        return Mono.just(environment);
    }

    @Override
    public Mono<LabeledRepository> buildStoreInternal(String key, Storable store, Environment environment) {
        LabeledRepository repository = getCache().get(key, s -> {
            log.debug("Initializing remote repository for label '{}'", key);


            if (Objects.nonNull(this.meterRegistry)) {
                meterRegistry.counter("graph.store.repository", "method", "init", "mode", "remote", "label", key).increment();
            }

            String updateEndpoint = "";
            String queryEndpoint = "";

            LabeledRepository labeledRepository = new LabeledRepository(key, new SPARQLRepository(queryEndpoint, updateEndpoint));
            labeledRepository.init();
            return labeledRepository;
        });

        return Mono.just(repository);
    }
}
