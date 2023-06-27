package org.av360.maverick.graph.services;

import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.store.TransactionsStore;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface TransactionsService {

    /**
     * Lists all transactions
     */
    Flux<Transaction> list(Integer limit, Integer offset, SessionContext authentication);


    Mono<Transaction> find(String identifier, SessionContext authentication);

    TransactionsStore getStore(SessionContext ctx);

    Mono<List<Transaction>> save(List<Transaction> transactions, Environment environment);
}
