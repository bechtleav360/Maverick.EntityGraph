package org.av360.maverick.graph.store.repository;

import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.store.behaviours.Storable;
import reactor.core.publisher.Mono;

public interface StoreBuilder {


    boolean canBuild(Environment environment);

    Mono<GraphStore> buildStore(Storable store, Environment environment);

    Mono<Void> shutdownStore(Storable store, Environment environment);
}
