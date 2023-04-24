package org.av360.maverick.graph.services;

import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.store.RepositoryType;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.sparqlbuilder.core.query.ConstructQuery;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Flux;

public interface QueryServices {
    default Flux<BindingSet> queryValues(String query, Authentication authentication) {
        return this.queryValues(query, authentication, RepositoryType.ENTITIES);
    }

    default Flux<BindingSet> queryValues(SelectQuery query, Authentication authentication){
        return this.queryValues(query, authentication, RepositoryType.ENTITIES);
    }


    default Flux<AnnotatedStatement> queryGraph(String query, Authentication authentication){
        return this.queryGraph(query, authentication, RepositoryType.ENTITIES);
    }


    default Flux<AnnotatedStatement> queryGraph(ConstructQuery query, Authentication authentication){
        return this.queryGraph(query, authentication, RepositoryType.ENTITIES);
    }



    Flux<BindingSet> queryValues(String query, Authentication authentication, RepositoryType repositoryType);

    Flux<BindingSet> queryValues(SelectQuery query, Authentication authentication, RepositoryType repositoryType);

    Flux<AnnotatedStatement> queryGraph(String query, Authentication authentication, RepositoryType repositoryType);

    Flux<AnnotatedStatement> queryGraph(ConstructQuery query, Authentication authentication, RepositoryType repositoryType);



}
