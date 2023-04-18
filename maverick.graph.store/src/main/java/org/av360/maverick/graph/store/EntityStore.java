package org.av360.maverick.graph.store;

import org.av360.maverick.graph.model.rdf.NamespaceAwareStatement;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.store.behaviours.*;
import org.av360.maverick.graph.store.rdf.fragments.RdfEntity;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
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

    Mono<RdfEntity> getEntity(Resource id, Authentication authentication, GrantedAuthority requiredAuthority, int includeNeighborsLevel);




    default Mono<Void> reset(Authentication authentication, GrantedAuthority requiredAuthority) {
        return this.reset(authentication, RepositoryType.ENTITIES, Authorities.SYSTEM);
    }

    default Flux<BindingSet> query(String query, Authentication authentication) {
        return this.query(query, authentication, Authorities.READER);
    }

    default Mono<RdfTransaction> commit(RdfTransaction trx, Authentication authentication) {
        return this.commit(trx, authentication, Authorities.READER);
    }

    default Mono<RdfEntity> getEntity(Resource entityIdentifier, Authentication authentication, int includeNeighborsLevel) {
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

    default Flux<RdfTransaction> commit(List<RdfTransaction> transactions, Authentication authentication) {
        return this.commit(transactions, authentication, Authorities.READER);
    }


}
