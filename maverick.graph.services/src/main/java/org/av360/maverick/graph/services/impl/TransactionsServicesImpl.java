package org.av360.maverick.graph.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.services.TransactionsService;
import org.av360.maverick.graph.services.config.RequiresPrivilege;
import org.av360.maverick.graph.store.PersistedTransactionsGraph;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Slf4j(topic = "graph.srvc.trx")
public class TransactionsServicesImpl implements TransactionsService {

    private final PersistedTransactionsGraph transactionsStore;

    public TransactionsServicesImpl(PersistedTransactionsGraph transactionsStore) {
        this.transactionsStore = transactionsStore;
    }

    @Override
    @RequiresPrivilege(Authorities.MAINTAINER_VALUE)
    public Flux<Transaction> list(Integer limit, Integer offset, SessionContext authentication) {
        return Flux.error(NotImplementedException::new);
    }

    @Override
    @RequiresPrivilege(Authorities.MAINTAINER_VALUE)
    public Mono<Transaction> find(String identifier, SessionContext authentication) {
        return Mono.error(NotImplementedException::new);
    }

    @Override
    @RequiresPrivilege(Authorities.MAINTAINER_VALUE)
    public PersistedTransactionsGraph getStore(SessionContext ctx) {
        return transactionsStore;
    }

    @Override
    @RequiresPrivilege(Authorities.MAINTAINER_VALUE)
    public Mono<List<Transaction>> save(List<Transaction> transactions, SessionContext context) {
        return this.transactionsStore.store(transactions, context.getEnvironment()).collectList();
    }
}
