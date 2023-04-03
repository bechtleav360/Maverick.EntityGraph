package org.av360.maverick.graph.services.impl;

import org.av360.maverick.graph.services.TransactionsService;
import org.av360.maverick.graph.store.rdf.models.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
@Service
@Slf4j(topic = "graph.srvc.trx")
public class TransactionsServicesImpl implements TransactionsService {
    @Override
    public Flux<Transaction> list(Integer limit, Integer offset, Authentication authentication) {
        return Flux.error(NotImplementedException::new);
    }

    @Override
    public Mono<Transaction> find(String identifier, Authentication authentication) {
        return Mono.error(NotImplementedException::new);
    }
}
