package org.av360.maverick.graph.store.rdf4j.repository;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.store.RepositoryType;
import org.av360.maverick.graph.store.TransactionsStore;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.av360.maverick.graph.store.rdf4j.repository.util.AbstractRepository;
import org.slf4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

@Slf4j(topic = "graph.repo.transactions")
@Component
public class TransactionsRepository extends AbstractRepository implements TransactionsStore {


    public TransactionsRepository() {
        super(RepositoryType.TRANSACTIONS);
    }

    @Override
    public Mono<RdfTransaction> store(RdfTransaction transaction, Authentication authentication, GrantedAuthority requiredAuthority) {
        return this.store(List.of(transaction), authentication, requiredAuthority).singleOrEmpty();
    }


    @Override
    public Flux<RdfTransaction> store(Collection<RdfTransaction> transactions, Authentication authentication, GrantedAuthority requiredAuthority) {
        return this.executeMany(authentication, requiredAuthority, connection -> {
            transactions.forEach(trx -> {
                try {
                    connection.begin();
                    connection.add(trx.getModel());
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
}
