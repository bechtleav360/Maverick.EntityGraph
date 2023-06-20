package org.av360.maverick.graph.services;

import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.store.TransactionsStore;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TransactionsService {

    /**
     * Lists all transactions
     */
    Flux<RdfTransaction> list(Integer limit, Integer offset, SessionContext authentication);


    Mono<RdfTransaction> find(String identifier, SessionContext authentication);

    TransactionsStore getStore(SessionContext ctx);
}
