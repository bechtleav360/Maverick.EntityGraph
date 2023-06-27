package org.av360.maverick.graph.store.behaviours;

import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.entities.Transaction;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
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

    Mono<Transaction> addStatement(Resource subject, IRI predicate, Value literal, Transaction transaction);

    Mono<Transaction> addStatement(Resource subject, IRI predicate, Value literal, Resource context, Transaction transaction);


    default Mono<Transaction> addStatement(Resource subject, IRI predicate, Value literal) {
        return this.addStatement(subject, predicate, literal);
    }



}
