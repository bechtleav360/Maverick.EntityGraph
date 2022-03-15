package com.bechtle.eagl.graph.repository;

import com.bechtle.eagl.graph.domain.model.extensions.NamespaceAwareStatement;
import com.bechtle.eagl.graph.domain.model.wrapper.Entity;
import com.bechtle.eagl.graph.domain.model.wrapper.Transaction;
import com.bechtle.eagl.graph.repository.behaviours.ModelUpdates;
import com.bechtle.eagl.graph.repository.behaviours.Resettable;
import com.bechtle.eagl.graph.repository.behaviours.Selectable;
import org.eclipse.rdf4j.model.*;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
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
