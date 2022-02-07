package com.bechtle.eagl.graph.repository;

import com.bechtle.eagl.graph.model.NamespaceAwareStatement;
import com.bechtle.eagl.graph.model.wrapper.Entity;
import com.bechtle.eagl.graph.model.wrapper.Transaction;
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
    Mono<Transaction> store(Model model, Transaction transaction);

    default  Mono<Transaction> store(Model model) {
        return this.store(model, new Transaction());
    }

    Mono<Transaction> store(Resource subject, IRI predicate, Value literal, Transaction transaction);

    default Mono<Transaction> store(Resource subject, IRI predicate, Value literal) {
        return this.store(subject, predicate, literal, new Transaction());
    }

    Mono<Entity> get(IRI id);

    Mono<TupleQueryResult> queryValues(String query);

    Flux<NamespaceAwareStatement> queryStatements(String query);

    ValueFactory getValueFactory();

    /**
     * Checks whether an entity with the given identity exists, ie. we have an rdf:type statement.
     * @param subj the id of the entity
     * @return true if exists
     */
    boolean existsSync(Resource subj);

    /**
     * Checks whether an entity with the given identity exists, ie. we have an rdf:type statement.
     * @param subj the id of the entity
     * @return true if exists
     */
    Mono<Boolean> exists(Resource subj);

    /**
     * Returns the type of the entity identified by the given id;
     * @param identifier the id of the entity
     * @return its type (or empty)
     */
    Mono<Value> type(Resource identifier);
}
