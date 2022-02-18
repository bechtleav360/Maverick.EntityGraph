package com.bechtle.eagl.graph.repository;

import com.bechtle.eagl.graph.domain.model.wrapper.Transaction;
import reactor.core.publisher.Mono;

public interface TransactionsStore {

    Mono<Transaction> store(Transaction transaction);

}
