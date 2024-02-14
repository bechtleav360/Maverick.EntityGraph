package org.av360.maverick.graph.store.behaviours;

import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.sparqlbuilder.core.query.ConstructQuery;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface Searchable extends RepositoryBehaviour {


    Flux<BindingSet> query(String q, Environment environment);

    Flux<AnnotatedStatement> construct(String q, Environment environment);

    Mono<Void> update(String query, Environment environment);

    default Flux<BindingSet> query(SelectQuery q, Environment environment) {
        return this.query(q.getQueryString(), environment);
    }

    default Flux<AnnotatedStatement> query(ConstructQuery q, Environment environment) {
        return this.construct(q.getQueryString(), environment);
    }



}
