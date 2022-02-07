package com.bechtle.eagl.graph.services;

import com.bechtle.eagl.graph.model.SimpleIRI;
import com.bechtle.eagl.graph.model.vocabulary.Transactions;
import com.bechtle.eagl.graph.model.wrapper.Entity;
import com.bechtle.eagl.graph.model.wrapper.IncomingStatements;
import com.bechtle.eagl.graph.model.NamespaceAwareStatement;
import com.bechtle.eagl.graph.model.wrapper.Transaction;
import com.bechtle.eagl.graph.repository.Graph;
import com.bechtle.eagl.graph.repository.Schema;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EntityServices {

    private final Graph graph;
    private final Schema schema;

    public EntityServices(Graph graph, Schema schema) {
        this.graph = graph;
        this.schema = schema;
    }

    public Mono<Entity> readEntity(String identifier) {
        return  graph.get(SimpleIRI.withDefaultNamespace(identifier));
    }



    public Mono<Transaction> createEntity(IncomingStatements triples) throws IOException {

        for(Resource obj : new ArrayList<>(triples.getModel().subjects())) {

            /* check if each node object has a valid type definition */
            if(! triples.getModel().contains(obj, RDF.TYPE, null)) {
                log.error("The object {} is missing a type", obj);
                throw new InvalidObjectException("Missing type definition for object");
            }

            /* TODO: check if create of resource of given type is supported or is it delegated to connector */

            /* Handle Ids */
            if(obj.isBNode()) {
                // generate a new qualified identifier if it is an anonymous node

                triples.generateName(obj);
            } else {
                // TODO: otherwise check if id already exists in graph
            }



            /* TODO: separate into different contexts by prefix */

        };

        return graph.store(triples.getModel());
    }

    /**
     * Saves a value, return an transaction
     * @param id entity id
     * @param predicatePrefix prefix of the predicate's namespace
     * @param predicateKey key of the predicate
     * @param value value
     * @return transaction model
     */
    public Mono<Transaction> setValue(String id, String predicatePrefix, String predicateKey, String value) {
        ValueFactory vf = this.graph.getValueFactory();
        SimpleIRI entityIdentifier = SimpleIRI.withDefaultNamespace(id);
        String namespace = schema.getNamespaceFor(predicatePrefix).orElseThrow().getName();

        return this.addStatement(entityIdentifier,
                SimpleIRI.withDefinedNamespace(namespace, predicateKey),
                vf.createLiteral(value)
                );
    }

    /**
     * Adds a statements
     */
    public Mono<Transaction> addStatement(Resource subj, IRI predicate, Value value) {
        return this.addStatement(subj, predicate, value, new Transaction());
    }

    /**
     * Adds a statements
     */
    public Mono<Transaction> addStatement(Resource subj, IRI predicate, Value value, Transaction transaction) {
        return this.graph.store(subj, predicate, value, transaction);
    }

    /**
     * Adds the entity to the model and creates a connection from the entity to the new entity
     *
     * If the relation is bidirectional, we should also create the inverse edge
     *
     * @param id entity id
     * @param predicatePrefix prefix of the predicate's namespace
     * @param predicateKey key of the predicate
     * @param linkedEntities value
     * @return transaction model
     */
    public Mono<Transaction> link(String id, String predicatePrefix, String predicateKey, IncomingStatements linkedEntities) throws IOException {

        SimpleIRI entityIdentifier = SimpleIRI.withDefaultNamespace(id);
        String namespace = schema.getNamespaceFor(predicatePrefix).orElseThrow().getName();
        IRI predicate = SimpleIRI.withDefinedNamespace(namespace, predicateKey);

        /*
            Constraints to check:
                the incoming model can contain multiple entities, all will be linked to
                the model cannot contain the link statements itself (we are creating them)


         */

        return this.createEntity(linkedEntities)
                .map(transaction -> {
                    transaction.listModifiedResources().forEach(value -> {
                        this.addStatement(entityIdentifier, predicate, value, transaction);
                    });
                    return transaction;
                });


        // FIXME: we should separate by entities (and have them as individual transactions)



    }
}
