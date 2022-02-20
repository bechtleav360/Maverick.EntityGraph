package com.bechtle.eagl.graph.repository;

import com.bechtle.eagl.graph.domain.model.extensions.NamespaceAwareStatement;
import com.bechtle.eagl.graph.domain.model.wrapper.Entity;
import com.bechtle.eagl.graph.domain.model.wrapper.Transaction;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;


public interface EntityStore {


    /**
     * Stores the triples.
     *
     * Can store multiple entities.
     *
     * @param model the statements to store
     * @return  Returns the transaction statements
     */
    Mono<Transaction> insertModel(Model model, Transaction transaction);

    Mono<Transaction> addStatement(Resource subject, IRI predicate, Value literal, Transaction transaction);

    default Mono<Transaction> addStatement(Resource subject, IRI predicate, Value literal) {
        return this.addStatement(subject, predicate, literal, new Transaction());
    }

    Mono<Entity> get(IRI id);

    Mono<TupleQueryResult> queryValues(String query);

    Mono<TupleQueryResult> queryValues(SelectQuery all);

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

    Mono<Void> reset();

    Mono<Void> importStatements(Publisher<DataBuffer> bytes, String mimetype);

    Mono<List<Statement>> listStatements(Resource value, IRI predicate, Value object);

    Mono<Transaction> removeStatement(Resource subject, IRI predicate, Value value, Transaction transaction);

    Mono<Transaction> deleteModel(List<Statement> subjectStatements);

    Mono<Transaction> deleteModel(List<Statement> statements, Transaction transaction);

    Flux<Transaction> commit(Collection<Transaction> transactions);

    default Mono<Transaction> commit(Transaction transaction) {
        return this.commit(List.of(transaction)).singleOrEmpty();
    }
}
