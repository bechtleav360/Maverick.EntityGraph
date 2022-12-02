package io.av360.maverick.graph.store.behaviours;


import io.av360.maverick.graph.model.security.Authorities;
import io.av360.maverick.graph.store.rdf.models.Transaction;
import io.av360.maverick.graph.store.RepositoryBuilder;
import io.av360.maverick.graph.store.RepositoryType;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public interface RepositoryBehaviour {


    default ValueFactory getValueFactory() {
        return SimpleValueFactory.getInstance();
    }

    default ValueFactory getValueFactory(@Nullable Authentication authentication) throws IOException {
        if (authentication == null || !authentication.isAuthenticated()) return this.getValueFactory();
        return getBuilder().buildRepository(this.getRepositoryType(), authentication).getValueFactory();
    }

    /**
     * Checks whether an entity with the given identity exists, ie. we have an crdf:type statement.
     *
     * @param subj the id of the entity
     * @return true if exists
     */
    default Mono<Boolean> exists(Resource subj, Authentication authentication) throws IOException {
        return this.exists(subj, authentication, Authorities.READER);
    }

    Mono<Boolean> exists(Resource subj, Authentication authentication, GrantedAuthority requiredAuthority) throws IOException;


    Flux<Transaction> commit(Collection<Transaction> transactions, Authentication authentication, GrantedAuthority requiredAuthority);

    default Mono<Transaction> commit(Transaction transaction, Authentication authentication, GrantedAuthority requiredAuthority) {
        return this.commit(List.of(transaction), authentication, requiredAuthority).singleOrEmpty();
    }


    default RepositoryConnection getConnection(Authentication authentication, GrantedAuthority requiredAuthority) throws IOException {
        return this.getConnection(authentication, getRepositoryType(), requiredAuthority);
    }

    default RepositoryConnection getConnection(Authentication authentication, RepositoryType repositoryType, GrantedAuthority requiredAuthority) throws IOException {
        if(! Authorities.satisfies(requiredAuthority, authentication.getAuthorities())) {
            throw new InsufficientAuthenticationException(String.format("Missing authority '%s' for initializing connection to requested repository for authentication", requiredAuthority.getAuthority()));
        }
        return getBuilder().buildRepository(repositoryType, authentication).getConnection();
    }

    RepositoryType getRepositoryType();

    RepositoryBuilder getBuilder();

}
