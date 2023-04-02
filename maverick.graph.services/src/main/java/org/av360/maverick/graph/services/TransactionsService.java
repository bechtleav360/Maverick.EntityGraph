package org.av360.maverick.graph.services;

import org.av360.maverick.graph.store.rdf.models.Transaction;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TransactionsService {

    /**
     * Lists all transactions
     */
    Flux<Transaction> list(Integer limit, Integer offset, Authentication authentication);


    Mono<Transaction> find(String identifier, Authentication authentication);
}
