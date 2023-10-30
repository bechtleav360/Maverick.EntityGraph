package org.av360.maverick.graph.store.behaviours;

import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.entities.Transaction;
import org.eclipse.rdf4j.model.*;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface StatementsAware extends TripleStore {

    default Mono<Transaction> removeStatement(Resource subject, IRI predicate, Value value, Transaction transaction) {
        return this.removeStatement(getValueFactory().createStatement(subject, predicate, value), transaction);
    }

    default Mono<Transaction> removeStatement(Statement statement, Transaction transaction) {
        return this.removeStatements(List.of(statement), transaction);
    }

    Mono<Set<Statement>> listStatements(Resource subject, IRI predicate, Value object, Environment environment);

    Mono<Transaction> removeStatements(Collection<Statement> statements, Transaction transaction);



    Mono<Transaction> addStatement(Resource subject, IRI predicate, Value literal, Resource context, Transaction transaction);

    default Mono<Transaction> addStatement(Resource subject, IRI predicate, Value literal) {
        return this.addStatement(subject, predicate, literal);
    }
    default  Mono<Transaction> addStatement(Resource subject, IRI predicate, Value literal, Transaction transaction) {
        return this.addStatement(subject, predicate, literal, null, transaction);
    }

    default Mono<Transaction> addStatement(Statement statement, Transaction transaction) {
        return this.addStatement(statement.getSubject(), statement.getPredicate(), statement.getObject(), statement.getContext(), transaction);
    }

    default Mono<Transaction> addStatement(Triple triple, Transaction transaction) {
        return this.addStatement(triple.getSubject(), triple.getPredicate(), triple.getObject(), transaction);
    }



}
