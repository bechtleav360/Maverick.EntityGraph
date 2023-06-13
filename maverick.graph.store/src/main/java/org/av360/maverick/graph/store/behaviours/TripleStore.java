package org.av360.maverick.graph.store.behaviours;


import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.store.RepositoryBuilder;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.slf4j.Logger;
import org.springframework.security.core.GrantedAuthority;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Set;

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
    default Mono<Boolean> exists(Resource subj, SessionContext ctx) {
        return this.exists(subj, ctx, Authorities.READER);
    }

    Mono<Boolean> exists(Resource subj, SessionContext ctx, GrantedAuthority requiredAuthority);


    Flux<IRI> types(Resource subj, SessionContext ctx, GrantedAuthority requiredAuthority);

    Flux<RdfTransaction> commit(Collection<RdfTransaction> transactions, SessionContext ctx, GrantedAuthority requiredAuthority, boolean merge);

    default Flux<RdfTransaction> commit(Collection<RdfTransaction> transactions, SessionContext ctx, GrantedAuthority requiredAuthority) {
        return this.commit(transactions, ctx, requiredAuthority, false);
    }

    default Flux<RdfTransaction> commit(List<RdfTransaction> transactions, SessionContext ctx) {
        return this.commit(transactions, ctx, Authorities.CONTRIBUTOR);
    }

    default Mono<RdfTransaction> commit(RdfTransaction trx, SessionContext ctx) {
        return this.commit(trx, ctx, Authorities.CONTRIBUTOR);
    }

    default Mono<RdfTransaction> commit(RdfTransaction transaction, SessionContext ctx, GrantedAuthority requiredAuthority) {
        return this.commit(List.of(transaction), ctx, requiredAuthority).singleOrEmpty();
    }

    Mono<Set<Statement>> listStatements(Resource subject, IRI predicate, Value object, SessionContext ctx, GrantedAuthority requiredAuthority);

    Mono<RdfTransaction> removeStatements(Collection<Statement> statements, RdfTransaction transaction);

    Mono<Boolean> hasStatement(Resource subject, IRI predicate, Value object, SessionContext ctx, GrantedAuthority requiredAuthority);

    RepositoryType getRepositoryType();

    RepositoryBuilder getBuilder();

}
