package org.av360.maverick.graph.store;

import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.store.behaviours.Maintainable;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.springframework.security.core.GrantedAuthority;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public interface TransactionsStore extends Maintainable {


    Mono<RdfTransaction> store(RdfTransaction transaction, SessionContext ctx, GrantedAuthority requiredAuthority);


    Flux<RdfTransaction> store(Collection<RdfTransaction> transaction, SessionContext ctx, GrantedAuthority requiredAuthority);

    default Flux<RdfTransaction> store(Collection<RdfTransaction> transaction, SessionContext ctx) {
        return this.store(transaction, ctx, Authorities.CONTRIBUTOR);
    }

    default Mono<RdfTransaction> store(RdfTransaction transaction, SessionContext ctx) {
        return this.store(transaction, ctx, Authorities.CONTRIBUTOR);
    }

}
