package org.av360.maverick.graph.store.behaviours;

import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.store.RepositoryType;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import reactor.core.publisher.Flux;


public interface Searchable extends RepositoryBehaviour {

    default Flux<AnnotatedStatement> construct(String query, Authentication authentication) {
        return this.construct(query, authentication, Authorities.READER, this.getRepositoryType());
    }

    default Flux<AnnotatedStatement> construct(String query, Authentication authentication, RepositoryType repositoryType) {
        return this.construct(query, authentication, Authorities.READER, repositoryType);
    }

    Flux<AnnotatedStatement> construct(String query, Authentication authentication, GrantedAuthority requiredAuthority, RepositoryType repositoryType);

    default Flux<BindingSet> query(String query, Authentication authentication) {
        return this.query(query, authentication, Authorities.READER, this.getRepositoryType());
    }

    default Flux<BindingSet> query(String query, Authentication authentication, RepositoryType repositoryType) {
        return this.query(query, authentication, Authorities.READER, repositoryType);
    }

    default Flux<BindingSet> query(SelectQuery all, Authentication authentication, GrantedAuthority requiredAuthority, RepositoryType repositoryType) {
        return this.query(all.getQueryString(), authentication, requiredAuthority, repositoryType);
    }

    default Flux<BindingSet> query(SelectQuery query, Authentication authentication, GrantedAuthority requiredAuthority) {
        return this.query(query, authentication, requiredAuthority, this.getRepositoryType());
    }

    Flux<BindingSet> query(String queryString, Authentication authentication, GrantedAuthority requiredAuthority, RepositoryType repositoryType);





}
