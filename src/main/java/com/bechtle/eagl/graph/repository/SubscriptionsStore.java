package com.bechtle.eagl.graph.repository;

import com.bechtle.eagl.graph.repository.behaviours.Selectable;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import reactor.core.publisher.Mono;

public interface SubscriptionsStore extends Selectable {

    Mono<Void> store(Model model);

}
