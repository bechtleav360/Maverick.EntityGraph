package com.bechtle.cougar.graph.repository.rdf4j.repository;

import com.bechtle.cougar.graph.repository.rdf4j.config.RepositoryConfiguration;
import com.bechtle.cougar.graph.domain.model.wrapper.Transaction;
import com.bechtle.cougar.graph.repository.TransactionsStore;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

@Slf4j
@Component
public class TransactionsRepository extends AbstractRepository implements TransactionsStore {


    public TransactionsRepository() {
        super(RepositoryConfiguration.RepositoryType.TRANSACTIONS);
    }

    @Override
    public Mono<Transaction> store(Transaction transaction) {
        return  this.store(List.of(transaction)).singleOrEmpty();
    }


    @Override
    public Flux<Transaction> store(Collection<Transaction> transactions) {
        return getRepository().flatMapMany(repository -> {
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


                } catch (RepositoryException e) {
                    log.error("Failed to initialize repository connection");
                    c.error(e);
                }

            });
        });



    }
}
