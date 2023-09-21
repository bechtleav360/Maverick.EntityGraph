package org.av360.maverick.graph.store.rdf4j.repository.builder;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.store.behaviours.Storable;
import org.av360.maverick.graph.store.rdf4j.extensions.LabeledRepository;
import org.av360.maverick.graph.store.rdf4j.repository.builder.base.MonitoredStoreBuilder;
import org.av360.maverick.graph.store.repository.StoreBuilder;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.lmdb.LmdbStore;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Objects;

@Service
@Slf4j(topic = "graph.repo.builder")
public class FilesystemStoreBuilder extends MonitoredStoreBuilder implements StoreBuilder {
    @Override
    public boolean canBuild(Environment environment) {
        boolean result = environment.isFlagged(Environment.RepositoryFlag.PERSISTENT)
                && !environment.isFlagged(Environment.RepositoryFlag.REMOTE);

        return result;
    }

    @Override
    public Mono<Environment> validateInternal(Storable store, Environment environment) {
        Validate.notNull(store.getDefaultStorageDirectory(), "Missing path in store");
        Validate.notBlank(store.getDefaultStorageDirectory(), "Empty path in store");

        // TODO: check application path, or set default path for environment
        return Mono.just(environment);
    }


    @Override
    public Mono<LabeledRepository> buildStoreInternal(String label, Storable store, Environment environment) {
        LabeledRepository repository = getCache().get(label, s -> {

            String path = store.getDefaultStorageDirectory();
            try {
                log.debug("Initializing persistent repository in path '{}' for label '{}'", path, label);


                if (Objects.nonNull(this.meterRegistry)) {
                    meterRegistry.counter("graph.store.repository", "method", "init", "mode", "persistent", "label", label).increment();
                }

                Resource file = new FileSystemResource(path);
                LmdbStoreConfig config = new LmdbStoreConfig();
                config.setTripleIndexes("spoc,ospc,psoc");

                if (!file.exists() && !file.getFile().mkdirs())
                    throw new IOException("Failed to create path: " + file.getFile());

                LabeledRepository labeledRepository = new LabeledRepository(label, new SailRepository(new LmdbStore(file.getFile(), config)));
                labeledRepository.init();
                return labeledRepository;
            } catch (RepositoryException | IOException e) {
                log.error("Failed to initialize persistent repository in path '{}'.", path, e);
                return null;
            }


        });

        return Mono.just(repository);
    }


}
