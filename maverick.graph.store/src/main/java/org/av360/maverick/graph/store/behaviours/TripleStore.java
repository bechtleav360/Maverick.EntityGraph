package org.av360.maverick.graph.store.behaviours;


import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.store.RepositoryBuilder;
import org.av360.maverick.graph.store.RepositoryType;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.slf4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

public interface TripleStore {

    Logger getLogger();

    String getDirectory();

    default ValueFactory getValueFactory() {
        return SimpleValueFactory.getInstance();
    }


    /**
     * Checks whether an entity with the given identity exists, ie. we have an crdf:type statement.
     *
     * @param subj the id of the entity
     * @return true if exists
     */
    default Mono<Boolean> exists(Resource subj, Authentication authentication) {
        return this.exists(subj, authentication, Authorities.READER);
    }

    Mono<Boolean> exists(Resource subj, Authentication authentication, GrantedAuthority requiredAuthority);


    Flux<IRI> types(Resource subj, Authentication authentication, GrantedAuthority requiredAuthority);

    Flux<RdfTransaction> commit(Collection<RdfTransaction> transactions, Authentication authentication, GrantedAuthority requiredAuthority, boolean merge);

    default Flux<RdfTransaction> commit(Collection<RdfTransaction> transactions, Authentication authentication, GrantedAuthority requiredAuthority) {
        return this.commit(transactions, authentication, requiredAuthority, false);
    }

    default Flux<RdfTransaction> commit(List<RdfTransaction> transactions, Authentication authentication) {
        return this.commit(transactions, authentication, Authorities.CONTRIBUTOR);
    }

    default Mono<RdfTransaction> commit(RdfTransaction trx, Authentication authentication) {
        return this.commit(trx, authentication, Authorities.CONTRIBUTOR);
    }

    default Mono<RdfTransaction> commit(RdfTransaction transaction, Authentication authentication, GrantedAuthority requiredAuthority) {
        return this.commit(List.of(transaction), authentication, requiredAuthority).singleOrEmpty();
    }



    RepositoryType getRepositoryType();

    RepositoryBuilder getBuilder();

}
