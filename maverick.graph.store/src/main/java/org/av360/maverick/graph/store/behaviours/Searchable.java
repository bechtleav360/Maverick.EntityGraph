package org.av360.maverick.graph.store.behaviours;

import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.model.security.Authorities;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.springframework.security.core.GrantedAuthority;
import reactor.core.publisher.Flux;


public interface Searchable extends TripleStore {


    Flux<BindingSet> query(String queryString, SessionContext ctx, GrantedAuthority requiredAuthority);

    Flux<AnnotatedStatement> construct(String query, SessionContext ctx, GrantedAuthority requiredAuthority);

    default Flux<AnnotatedStatement> construct(String query, SessionContext ctx) {
        return this.construct(query, ctx, Authorities.READER);
    }

    default Flux<BindingSet> query(String query, SessionContext ctx) {
        return this.query(query, ctx, Authorities.READER);
    }


    default Flux<BindingSet> query(SelectQuery all, SessionContext ctx, GrantedAuthority requiredAuthority) {
        return this.query(all.getQueryString(), ctx, requiredAuthority);
    }







}
