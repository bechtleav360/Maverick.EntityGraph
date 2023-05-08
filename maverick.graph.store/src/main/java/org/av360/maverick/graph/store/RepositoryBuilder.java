package org.av360.maverick.graph.store;

import org.av360.maverick.graph.store.behaviours.TripleStore;
import org.av360.maverick.graph.store.rdf.LabeledRepository;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

public interface RepositoryBuilder {


    Mono<LabeledRepository> buildRepository(TripleStore store, Authentication authentication);


}
