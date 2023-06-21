package org.av360.maverick.graph.store.behaviours;

import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import reactor.core.publisher.Mono;

import java.util.List;

public interface StatementsAware extends TripleStore {

    default Mono<RdfTransaction> removeStatement(Resource subject, IRI predicate, Value value, RdfTransaction transaction) {
        return this.removeStatement(getValueFactory().createStatement(subject, predicate, value), transaction);
    }

    default Mono<RdfTransaction> removeStatement(Statement statement, RdfTransaction transaction) {
        return this.removeStatements(List.of(statement), transaction);
    }


    Mono<RdfTransaction> addStatement(Resource subject, IRI predicate, Value literal, RdfTransaction transaction);

    Mono<RdfTransaction> addStatement(Resource subject, IRI predicate, Value literal, Resource context, RdfTransaction transaction);


    default Mono<RdfTransaction> addStatement(Resource subject, IRI predicate, Value literal) {
        return this.addStatement(subject, predicate, literal);
    }



}
