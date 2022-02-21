package com.bechtle.eagl.graph.repository.rdf4j.repository;

import com.bechtle.eagl.graph.domain.model.wrapper.Transaction;
import com.bechtle.eagl.graph.repository.TransactionsStore;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

@Slf4j
@Component
public class TransactionsRepository implements TransactionsStore {

    private final Repository repository;

    public TransactionsRepository(@Qualifier("transactions-storage") Repository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Transaction> store(Transaction transaction) {
        return  this.store(List.of(transaction)).singleOrEmpty();
    }


    @Override
    public Flux<Transaction> store(Collection<Transaction> transactions) {

        return Flux.create(c -> {
            try (RepositoryConnection connection = repository.getConnection()) {
                transactions.forEach(trx -> {
                    if (trx == null) {
                        log.trace("Trying to store an empty transaction.");
                    } else {

                        try {

                            connection.begin();
                            connection.add(trx.getModel());
                            connection.commit();

                            c.next(trx);
                        } catch (Exception e) {
                            log.error("Error while storing transaction, performing rollback.", e);
                            connection.rollback();
                            c.error(e);
                        }
                    }



                });
                c.complete();


            }

        });
    }
}
