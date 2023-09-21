package org.av360.maverick.graph.store.rdf4j.repository;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.model.vocabulary.Transactions;
import org.av360.maverick.graph.store.PersistedTransactionsGraph;
import org.av360.maverick.graph.store.rdf4j.repository.util.SailStore;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Collection;

@Slf4j(topic = "graph.repo.transactions")
@Component
public class TransactionsStoreImpl extends SailStore implements PersistedTransactionsGraph {


    public TransactionsStoreImpl() {
        super(RepositoryType.TRANSACTIONS);
    }

    @Override
    public Flux<Transaction> store(Collection<Transaction> transactions, Environment environment) {
        return this.applyManyWithConnection(environment, connection -> {
            transactions.forEach(trx -> {
                try {
                    connection.begin();
                    connection.add(trx.get(Transactions.GRAPH_PROVENANCE));
                    connection.commit();
                } catch (Exception e) {
                    log.error("Error while storing transaction, performing rollback.", e);
                    connection.rollback();
                }
            });

            return transactions;
        });
    }


    @Override
    public Logger getLogger() {
        return log;
    }


    @Override
    protected void addDefaultStorageConfiguration(Environment environment) {

    }
}
