package org.av360.maverick.graph.store.behaviours;

import org.av360.maverick.graph.store.rdf.models.Transaction;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

public interface Statements extends RepositoryBehaviour {

    default Mono<Transaction> removeStatement(Resource subject, IRI predicate, Value value, Transaction transaction) {
        return this.removeStatement(getValueFactory().createStatement(subject, predicate, value), transaction);
    }

    default Mono<Transaction> removeStatement(Statement statement, Transaction transaction) {
        return this.removeStatements(List.of(statement), transaction);
    }


    Mono<Transaction> addStatement(Resource subject, IRI predicate, Value literal, Transaction transaction);

    default Mono<Transaction> addStatement(Resource subject, IRI predicate, Value literal) {
        return this.addStatement(subject, predicate, literal);
    }


    Mono<List<Statement>> listStatements(Resource value, IRI predicate, Value object, Authentication authentication, GrantedAuthority requiredAuthority);

    Mono<Transaction> removeStatements(Collection<Statement> statements, Transaction transaction);
}
