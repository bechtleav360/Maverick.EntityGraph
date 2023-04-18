package org.av360.maverick.graph.store;

import org.av360.maverick.graph.model.rdf.NamespaceAwareStatement;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.store.behaviours.*;
import org.av360.maverick.graph.store.rdf.models.Entity;
import org.av360.maverick.graph.store.rdf.models.Transaction;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;


public interface EntityStore extends Searchable, Resettable, ModelUpdates, Selectable, Statements {

    Mono<Entity> getEntity(Resource id, Authentication authentication, GrantedAuthority requiredAuthority, int includeNeighborsLevel);




    default Mono<Void> reset(Authentication authentication, GrantedAuthority requiredAuthority) {
        return this.reset(authentication, RepositoryType.ENTITIES, Authorities.SYSTEM);
    }

    default Flux<BindingSet> query(String query, Authentication authentication) {
        return this.query(query, authentication, Authorities.READER);
    }

    default Mono<Transaction> commit(Transaction trx, Authentication authentication) {
        return this.commit(trx, authentication, Authorities.READER);
    }

    default Mono<Entity> getEntity(Resource entityIdentifier, Authentication authentication, int includeNeighborsLevel) {
        return this.getEntity(entityIdentifier, authentication, Authorities.READER, includeNeighborsLevel);
    }

    default Mono<Set<Statement>> listStatements(IRI object, IRI predicate, Value val, Authentication authentication) {
        return this.listStatements(object, predicate, val, authentication, Authorities.READER);
    }

    default Mono<Set<Statement>> listStatements(Resource object, IRI predicate, Value val, Authentication authentication) {
        return this.listStatements(object, predicate, val, authentication, Authorities.READER);
    }

    default Flux<NamespaceAwareStatement> construct(String query, Authentication authentication) {
        return this.construct(query, authentication, Authorities.READER);
    }

    default Flux<Transaction> commit(List<Transaction> transactions, Authentication authentication) {
        return this.commit(transactions, authentication, Authorities.READER);
    }


}