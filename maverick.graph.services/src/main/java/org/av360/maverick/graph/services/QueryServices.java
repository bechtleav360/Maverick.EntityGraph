package org.av360.maverick.graph.services;

import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.sparqlbuilder.core.query.ConstructQuery;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import reactor.core.publisher.Flux;

public interface QueryServices {
    default Flux<BindingSet> queryValues(String query, SessionContext ctx) {
        return this.queryValues(query, ctx, RepositoryType.ENTITIES);
    }

    default Flux<BindingSet> queryValues(SelectQuery query, SessionContext ctx){
        return this.queryValues(query, ctx, RepositoryType.ENTITIES);
    }


    default Flux<AnnotatedStatement> queryGraph(String query, SessionContext ctx){
        return this.queryGraph(query, ctx, RepositoryType.ENTITIES);
    }


    default Flux<AnnotatedStatement> queryGraph(ConstructQuery query, SessionContext ctx){
        return this.queryGraph(query, ctx, RepositoryType.ENTITIES);
    }



    Flux<BindingSet> queryValues(String query, SessionContext ctx, RepositoryType repositoryType);

    Flux<BindingSet> queryValues(SelectQuery query, SessionContext ctx, RepositoryType repositoryType);

    Flux<AnnotatedStatement> queryGraph(String query, SessionContext ctx, RepositoryType repositoryType);

    Flux<AnnotatedStatement> queryGraph(ConstructQuery query, SessionContext ctx, RepositoryType repositoryType);



}
