package com.bechtle.eagl.graph.repository;

import com.bechtle.eagl.graph.domain.model.wrapper.Transaction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public interface TransactionsStore {

    Mono<Transaction> store(Transaction transaction);

    Flux<Transaction> store(Collection<Transaction> transaction);
}
