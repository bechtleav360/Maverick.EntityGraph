package org.av360.maverick.graph.store;

import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.store.rdf.LabeledRepository;
import reactor.core.publisher.Mono;

public interface RepositoryBuilder {


    // FIXME: remove Triplestore parameter, only required to get requested repository type, which should be part of session context
    Mono<LabeledRepository> buildRepository(EntityStore store, Environment environment);


    Mono<Void> shutdownRepository(EntityStore store, Environment environment);
}
