package org.av360.maverick.graph.store.behaviours;

import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import reactor.core.publisher.Flux;


public interface Searchable extends RepositoryBehaviour {

    default Flux<BindingSet> query(SelectQuery all, Authentication authentication, GrantedAuthority requiredAuthority) {
        return this.query(all.getQueryString(), authentication, requiredAuthority);
    }

    Flux<BindingSet> query(String queryString, Authentication authentication, GrantedAuthority requiredAuthority);


    Flux<AnnotatedStatement> construct(String query, Authentication authentication, GrantedAuthority requiredAuthority);
}
