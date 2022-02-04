package com.bechtle.eagl.graph.repository;

import com.bechtle.eagl.graph.model.NamespaceAwareStatement;
import com.bechtle.eagl.graph.model.SimpleIRI;
import com.bechtle.eagl.graph.model.Transaction;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.query.TupleQueryResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface Graph {


    /**
     * Stores the triples.
     *
     * Can store multiple entities.
     *
     * @param model the statements to store
     * @return  Returns the transaction statements
     */
    Flux<NamespaceAwareStatement> store(Model model);

    Flux<NamespaceAwareStatement> get(IRI id);

    Mono<TupleQueryResult> queryValues(String query);

    Flux<NamespaceAwareStatement> queryStatements(String query);

    ValueFactory getValueFactory();

    Flux<NamespaceAwareStatement> store(IRI subject, IRI predicate, Value literal);
}
