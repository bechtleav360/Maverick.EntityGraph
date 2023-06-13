package org.av360.maverick.graph.store;

import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.store.behaviours.*;
import org.av360.maverick.graph.store.rdf.fragments.RdfEntity;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.springframework.security.core.GrantedAuthority;
import reactor.core.publisher.Mono;

import java.util.Set;


public interface EntityStore extends Searchable, Maintainable, ModelAware, Selectable, StatementsAware {

    Mono<RdfEntity> getEntity(Resource id, SessionContext ctx, GrantedAuthority requiredAuthority, int includeNeighborsLevel);


    default Mono<RdfEntity> getEntity(Resource entityIdentifier, int includeNeighborsLevel, SessionContext ctx) {
        return this.getEntity(entityIdentifier, ctx, Authorities.READER, includeNeighborsLevel);
    }

    default Mono<Set<Statement>> listStatements(IRI object, IRI predicate, Value val, SessionContext ctx) {
        return this.listStatements(object, predicate, val, ctx, Authorities.READER);
    }

    default Mono<Set<Statement>> listStatements(Resource object, IRI predicate, Value val, SessionContext ctx) {
        return this.listStatements(object, predicate, val, ctx, Authorities.READER);
    }






}
