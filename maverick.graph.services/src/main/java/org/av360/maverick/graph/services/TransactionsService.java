package org.av360.maverick.graph.services;

import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TransactionsService {

    /**
     * Lists all transactions
     */
    Flux<RdfTransaction> list(Integer limit, Integer offset, Authentication authentication);


    Mono<RdfTransaction> find(String identifier, Authentication authentication);
}
