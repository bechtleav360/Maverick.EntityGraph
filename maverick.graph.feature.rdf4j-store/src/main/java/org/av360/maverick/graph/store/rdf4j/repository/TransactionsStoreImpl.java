package org.av360.maverick.graph.store.rdf4j.repository;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.model.vocabulary.meg.Transactions;
import org.av360.maverick.graph.store.TransactionsStore;
import org.av360.maverick.graph.store.behaviours.Maintainable;
import org.av360.maverick.graph.store.rdf.fragments.RdfFragment;
import org.av360.maverick.graph.store.rdf4j.repository.util.AbstractRdfRepository;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

@Slf4j(topic = "graph.repo.transactions")
@Component
public class TransactionsStoreImpl extends AbstractRdfRepository implements TransactionsStore, Maintainable {

    @Value("${application.storage.transactions.path:#{null}}")
    private String path;


    @Override
    public Flux<Transaction> store(Collection<Transaction> transactions, Environment environment) {
        return this.applyManyWithConnection(environment, connection -> {
            transactions.forEach(trx -> {
                try {
                    connection.begin();
                    connection.add(trx.getModel(Transactions.GRAPH_PROVENANCE));
                    connection.commit();
                } catch (Exception e) {
                    log.error("Error while storing transaction, performing rollback.", e);
                    connection.rollback();
                }
            });

            return transactions.stream();
        });
    }


    @Override
    public Logger getLogger() {
        return log;
    }

    @Override
    public String getDirectory() {
        return this.path;
    }


    @Override
    public Mono<Transaction> insertFragment(RdfFragment fragment, Environment environment) {
        return null;
    }
}
