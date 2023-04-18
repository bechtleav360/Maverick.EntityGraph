package org.av360.maverick.graph.store;

import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.store.behaviours.Resettable;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public interface TransactionsStore extends Resettable {


    Mono<RdfTransaction> store(RdfTransaction transaction, Authentication authentication, GrantedAuthority requiredAuthority);


    Flux<RdfTransaction> store(Collection<RdfTransaction> transaction, Authentication authentication, GrantedAuthority requiredAuthority);

    default Flux<RdfTransaction> store(Collection<RdfTransaction> transaction, Authentication authentication) {
        return this.store(transaction, authentication, Authorities.CONTRIBUTOR);
    }

    default Mono<RdfTransaction> store(RdfTransaction transaction, Authentication authentication) {
        return this.store(transaction, authentication, Authorities.CONTRIBUTOR);
    }

    default Mono<Void> reset(Authentication authentication, GrantedAuthority requiredAuthority) {
        return this.reset(authentication, RepositoryType.TRANSACTIONS, Authorities.SYSTEM);
    }
}
