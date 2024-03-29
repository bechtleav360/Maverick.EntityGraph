package org.av360.maverick.graph.model.entities;

import org.av360.maverick.graph.model.enums.Activity;
import org.av360.maverick.graph.model.errors.store.FailedTransactionException;
import org.av360.maverick.graph.model.rdf.Triples;
import org.av360.maverick.graph.model.vocabulary.meg.Transactions;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelCollector;
import org.eclipse.rdf4j.model.util.Statements;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

public interface Transaction extends Triples {


    Transaction inserts(Collection<Statement> statements);

    Transaction updates(Collection<Statement> statements);

    Transaction affects(Collection<Statement> statements);

    Transaction removes(Collection<Statement> statements);

    default Transaction affects(Resource subject, IRI predicate, Value value) {
        return this.affects(List.of(SimpleValueFactory.getInstance().createStatement(subject, predicate, value)));
    }

    default Transaction affects(Statement statement) {
        return this.affects(List.of(statement));
    }

    default Transaction inserts(Resource subject, IRI predicate, Value value) {
        return this.inserts(subject, predicate, value, null);
    }
    default Transaction updates(Resource subject, IRI predicate, Value value) {
        return this.updates(subject, predicate, value, null);
    }

    default Transaction updates(Resource subject, IRI predicate, Value value, Resource context) {
        return this.updates(Statements.statement(subject, predicate, value, context));
    }

    default Transaction updates(Statement statement) {
        return this.updates(List.of(statement));
    }

    default Transaction inserts(Statement statement) {
        return this.inserts(List.of(statement));
    }

    default Transaction inserts(Triple triple) {
        return this.inserts(Statements.statement(triple));
    }

    default Transaction inserts(Resource subject, IRI predicate, Value value, Resource context) {
        return this.inserts(Statements.statement(subject, predicate, value, context));
    }

    /**
     * Returns a (unmodifiable) subset of statements from a transaction within a specific context. The context attributes
     * are removed.
     * @param context
     * @return
     */
    default Model getModel(IRI context) {
        return this.getModel().filter(null, null, null, context)
                .stream()
                .map(statement -> SimpleValueFactory.getInstance().createStatement(statement.getSubject(), statement.getPredicate(), statement.getObject()))
                .collect(new ModelCollector())
                .unmodifiable();
    }

    Model getModel();

    default Model getRemovedStatements() {
        return this.getModel(Transactions.GRAPH_DELETED);
    }

    default Model getInsertedStatements() {
        return this.getModel(Transactions.GRAPH_CREATED);
    }
    default Model getAffectedStatements() {
        return this.getModel(Transactions.GRAPH_AFFECTED);
    }

    IRI getIdentifier();

    void setCompleted();

    void setFailed(String message);


    List<Value> affectedSubjects(Activity ... activities);

    boolean isCompleted();

    default Mono<Transaction> verifyCompleted() {
        if(this.isCompleted()) return Mono.just(this);
        else return Mono.error(new FailedTransactionException(this));

    }




    default Transaction forInsert(Statement statement) {
        return this.forInsert(List.of(statement));
    }

    default Transaction forInsert(Collection<Statement> statements) {
        return this.inserts(statements);
    }


    default Transaction removes(Statement statement) {
        return this.removes(List.of(statement));
    }
    default Transaction removes(IRI subject, IRI predicate, Value value) {
        return this.removes(Statements.statement(subject, predicate, value, null));
    }

    default Transaction removes(IRI subject, IRI predicate, Value value, Resource context) {
        return this.removes(Statements.statement(subject, predicate, value, context));
    }

}
