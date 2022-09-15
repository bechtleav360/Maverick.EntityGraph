package cougar.graph.store;

import cougar.graph.model.security.Authorities;
import cougar.graph.store.rdf.models.Transaction;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public interface TransactionsStore {



    Mono<Transaction> store(Transaction transaction, Authentication authentication, GrantedAuthority requiredAuthority);


    Flux<Transaction> store(Collection<Transaction> transaction, Authentication authentication, GrantedAuthority requiredAuthority);

    default Flux<Transaction> store(Collection<Transaction> transaction, Authentication authentication) {
        return this.store(transaction, authentication, Authorities.USER);
    }

    default Mono<Transaction> store(Transaction transaction, Authentication authentication) {
        return this.store(transaction, authentication, Authorities.USER);
    }
}
