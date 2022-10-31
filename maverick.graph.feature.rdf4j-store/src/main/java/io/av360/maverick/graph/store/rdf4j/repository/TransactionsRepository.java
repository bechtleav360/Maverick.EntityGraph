package io.av360.maverick.graph.store.rdf4j.repository;

import io.av360.maverick.graph.store.RepositoryType;
import io.av360.maverick.graph.store.TransactionsStore;
import io.av360.maverick.graph.store.rdf.models.Transaction;
import io.av360.maverick.graph.store.rdf4j.repository.util.AbstractRepository;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

@Slf4j
@Component
public class TransactionsRepository extends AbstractRepository implements TransactionsStore {


    public TransactionsRepository() {
        super(RepositoryType.TRANSACTIONS);
    }

    @Override
    public Mono<Transaction> store(Transaction transaction, Authentication authentication, GrantedAuthority requiredAuthority) {
        return this.store(List.of(transaction), authentication, requiredAuthority).singleOrEmpty();
    }



    @Override
    public Flux<Transaction> store(Collection<Transaction> transactions, Authentication authentication, GrantedAuthority requiredAuthority) {
        return Flux.create(c -> {
            try (RepositoryConnection connection = getConnection(authentication, requiredAuthority)) {
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
            } catch (IOException e) {
                c.error(e);
            }

        });

    }


}
