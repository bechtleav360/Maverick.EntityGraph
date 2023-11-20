package org.av360.maverick.graph.store.behaviours;


import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.Statements;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

public interface Commitable extends RepositoryBehaviour {



    Flux<Transaction> commit(Collection<Transaction> transactions, Environment environment, boolean merge);

    default Flux<Transaction> commit(Collection<Transaction> transactions, Environment environment) {
        return this.commit(transactions, environment, false);
    }

    default Mono<Transaction> commit(Transaction transaction, Environment environment) {
        return this.commit(List.of(transaction), environment).singleOrEmpty();
    }


    @Deprecated
    default Mono<Transaction> removeStatements(Collection<Statement> statements, Transaction transaction) {
        Assert.notNull(transaction, "Transaction cannot be null");

        Transaction trx = transaction
                .affects(statements)
                .removes(statements);
        return Mono.just(trx);
    }
    @Deprecated
    default Mono<Transaction> removeStatements(Collection<Statement> statements, Environment environment) {
        return this.removeStatements(statements, new RdfTransaction()).flatMap(transaction -> this.commit(transaction, environment));
    }

    @Deprecated
    default Mono<Transaction> insertStatements(Collection<Statement> statements, Transaction transaction) {
        Assert.notNull(transaction, "Transaction cannot be null");

        Transaction trx = transaction
                .affects(statements)
                .inserts(statements);
        return Mono.just(trx);
    }
    @Deprecated
    default Mono<Transaction> insertStatement(Resource subject, IRI predicate, Value literal, Resource context, Transaction transaction) {
        Assert.notNull(transaction, "Transaction cannot be null");

        Transaction trx = transaction
                .affects(subject, predicate, literal)
                .inserts(subject, predicate, literal, context);
        return Mono.just(trx);
    }
    @Deprecated
    default  Mono<Transaction> insertStatement(Resource subject, IRI predicate, Value literal, Transaction transaction) {
        return this.insertStatement(subject, predicate, literal, null, transaction);
    }
    @Deprecated
    default Mono<Transaction> insertStatement(Statement statement, Transaction transaction) {
        return this.insertStatement(statement.getSubject(), statement.getPredicate(), statement.getObject(), statement.getContext(), transaction);
    }
    @Deprecated
    default Mono<Transaction> insertStatement(Triple triple, Transaction transaction) {
        return this.insertStatement(triple.getSubject(), triple.getPredicate(), triple.getObject(), transaction);
    }


    default Mono<Transaction> markForRemoval(Resource subject, IRI predicate, Value value, Transaction transaction) {
        return this.markForRemoval(Statements.statement(subject, predicate, value, null), transaction);
    }

    default Mono<Transaction> markForRemoval(Statement statement, Transaction transaction) {
        return this.removeStatements(List.of(statement), transaction);
    }


}
