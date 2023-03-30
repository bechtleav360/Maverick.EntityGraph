package org.av360.maverick.graph.store;

import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.store.behaviours.Resettable;
import org.av360.maverick.graph.store.rdf.models.Transaction;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public interface TransactionsStore extends Resettable {


    Mono<Transaction> store(Transaction transaction, Authentication authentication, GrantedAuthority requiredAuthority);


    Flux<Transaction> store(Collection<Transaction> transaction, Authentication authentication, GrantedAuthority requiredAuthority);

    default Flux<Transaction> store(Collection<Transaction> transaction, Authentication authentication) {
        return this.store(transaction, authentication, Authorities.CONTRIBUTOR);
    }

    default Mono<Transaction> store(Transaction transaction, Authentication authentication) {
        return this.store(transaction, authentication, Authorities.CONTRIBUTOR);
    }

    default Mono<Void> reset(Authentication authentication, GrantedAuthority requiredAuthority) {
        return this.reset(authentication, RepositoryType.TRANSACTIONS, Authorities.SYSTEM);
    }
}
