package org.av360.maverick.graph.services;

import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.entities.Transaction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public interface TransactionsService {

    /**
     * Lists all transactions
     */
    Flux<Transaction> list(Integer limit, Integer offset, SessionContext context);


    Mono<Transaction> find(String identifier, SessionContext context);


    Flux<Transaction> save(Collection<Transaction> transactions, SessionContext context);

}
