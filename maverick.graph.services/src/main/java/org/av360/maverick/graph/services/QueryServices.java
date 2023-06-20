package org.av360.maverick.graph.services;

import org.apache.commons.lang3.Validate;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.sparqlbuilder.core.query.ConstructQuery;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import reactor.core.publisher.Flux;

public interface QueryServices {


    /**
     * Running a select query against the repository within the repository type
     *
     * @param query, the query
     * @param entities, the repository to search in. Is required, since it might differ from the one in the session context.
     * @param ctx, current cession context
     * @return Bindings
     */
    Flux<BindingSet> queryValues(String query, RepositoryType entities, SessionContext ctx);

    /**
     * Running a select query against the repository within the repository type
     *
     * @param query, the query
     * @param entities, the repository to search in. Is required, since it might differ from the one in the session context.
     * @param ctx, current cession context
     * @return Bindings
     */
    Flux<BindingSet> queryValues(SelectQuery query, RepositoryType entities, SessionContext ctx);

    default Flux<BindingSet>  queryValues(String query, SessionContext ctx) {
        Validate.notNull(ctx.getEnvironment().getRepositoryType());

        return this.queryValues(query, ctx.getEnvironment().getRepositoryType(), ctx);
    }

    default Flux<BindingSet>  queryValues(SelectQuery query, SessionContext ctx) {
        Validate.notNull(ctx.getEnvironment().getRepositoryType());

        return this.queryValues(query, ctx.getEnvironment().getRepositoryType(), ctx);
    }
    Flux<AnnotatedStatement> queryGraph(String query, RepositoryType entities,SessionContext ctx);


    Flux<AnnotatedStatement> queryGraph(ConstructQuery query, RepositoryType entities,SessionContext ctx);

    default Flux<AnnotatedStatement>  queryGraph(String query, SessionContext ctx) {
        Validate.notNull(ctx.getEnvironment().getRepositoryType());

        return this.queryGraph(query, ctx.getEnvironment().getRepositoryType(), ctx);
    }

    default Flux<AnnotatedStatement>  queryGraph(ConstructQuery query, SessionContext ctx) {
        Validate.notNull(ctx.getEnvironment().getRepositoryType());

        return this.queryGraph(query, ctx.getEnvironment().getRepositoryType(), ctx);
    }

}
