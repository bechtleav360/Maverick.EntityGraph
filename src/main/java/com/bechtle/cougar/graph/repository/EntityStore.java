package com.bechtle.cougar.graph.repository;

import com.bechtle.cougar.graph.repository.behaviours.ModelUpdates;
import com.bechtle.cougar.graph.repository.behaviours.Resettable;
import com.bechtle.cougar.graph.repository.behaviours.Selectable;
import com.bechtle.cougar.graph.domain.model.extensions.NamespaceAwareStatement;
import com.bechtle.cougar.graph.domain.model.wrapper.Entity;
import com.bechtle.cougar.graph.domain.model.wrapper.Transaction;
import org.eclipse.rdf4j.model.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;


public interface EntityStore extends Selectable, Resettable, ModelUpdates {




    Mono<Transaction> addStatement(Resource subject, IRI predicate, Value literal, Transaction transaction);

    default Mono<Transaction> addStatement(Resource subject, IRI predicate, Value literal) {
        return this.addStatement(subject, predicate, literal, new Transaction());
    }

    Mono<Entity> get(IRI id);


    Flux<NamespaceAwareStatement> constructQuery(String query);


    Mono<List<Statement>> listStatements(Resource value, IRI predicate, Value object);

    Mono<Transaction> removeStatement(Resource subject, IRI predicate, Value value, Transaction transaction);


}
