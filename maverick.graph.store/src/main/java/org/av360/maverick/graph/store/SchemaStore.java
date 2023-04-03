package org.av360.maverick.graph.store;

import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.store.behaviours.Resettable;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SchemaStore extends Resettable {

    ValueFactory getValueFactory();

    Flux<Namespace> listNamespaces();

    default Mono<Void> reset(Authentication authentication, GrantedAuthority requiredAuthority) {
        return this.reset(authentication, RepositoryType.SCHEMA, Authorities.SYSTEM);
    }
}
