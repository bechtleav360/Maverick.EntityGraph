package com.bechtle.eagl.graph.repository.rdf4j.repository;

import com.bechtle.eagl.graph.domain.model.wrapper.Transaction;
import com.bechtle.eagl.graph.repository.TransactionsStore;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class TransactionsRepository implements TransactionsStore {

    private final Repository repository;

    public TransactionsRepository(@Qualifier("transactions-storage") Repository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Transaction> store(Transaction transaction) {


        return Mono.create(c -> {
            if (transaction == null) c.success();

            try (RepositoryConnection connection = repository.getConnection()) {
                assert transaction != null;

                try {

                    connection.begin();
                    connection.add(transaction.getModel());
                    connection.commit();

                    c.success(transaction);
                } catch (Exception e) {
                    log.error("Error while storing transaction, performing rollback.", e);
                    connection.rollback();
                    c.error(e);
                }
            }

        });

    }
}
