package org.av360.maverick.graph.feature.applications.store;

import org.av360.maverick.graph.store.RepositoryType;
import org.av360.maverick.graph.store.behaviours.ModelUpdates;
import org.av360.maverick.graph.store.behaviours.Resettable;
import org.av360.maverick.graph.store.behaviours.Searchable;
import org.av360.maverick.graph.store.behaviours.Statements;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import reactor.core.publisher.Mono;

public interface ApplicationsStore extends Searchable, ModelUpdates, Resettable, Statements {

    default Mono<Void> reset(Authentication authentication, GrantedAuthority requiredAuthority) {
        return this.reset(authentication, RepositoryType.APPLICATION, requiredAuthority);
    }
}
