package org.av360.maverick.graph.services;

import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.sparqlbuilder.core.query.ConstructQuery;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import reactor.core.publisher.Flux;

public interface QueryServices {






    Flux<BindingSet> queryValues(String query, SessionContext ctx);

    Flux<BindingSet> queryValues(SelectQuery query, SessionContext ctx);

    Flux<AnnotatedStatement> queryGraph(String query, SessionContext ctx);

    Flux<AnnotatedStatement> queryGraph(ConstructQuery query, SessionContext ctx);



}
