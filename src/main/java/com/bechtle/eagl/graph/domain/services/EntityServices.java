package com.bechtle.eagl.graph.domain.services;

import com.bechtle.eagl.graph.domain.model.errors.MissingType;
import com.bechtle.eagl.graph.domain.model.extensions.EntityIRI;
import com.bechtle.eagl.graph.domain.model.errors.EntityExistsAlready;
import com.bechtle.eagl.graph.domain.model.errors.EntityNotFound;
import com.bechtle.eagl.graph.domain.model.wrapper.Entity;
import com.bechtle.eagl.graph.domain.model.wrapper.IncomingStatements;
import com.bechtle.eagl.graph.domain.model.wrapper.Transaction;
import com.bechtle.eagl.graph.repository.EntityStore;
import com.bechtle.eagl.graph.repository.SchemaStore;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;

@Slf4j
@Service
public class EntityServices {

    private final EntityStore graph;
    private final SchemaStore schema;

    public EntityServices(EntityStore graph, SchemaStore schema) {
        this.graph = graph;
        this.schema = schema;
    }

    public Mono<Entity> readEntity(String identifier) {
        return graph.get(EntityIRI.withDefaultNamespace(identifier))
                .switchIfEmpty(Mono.error(new EntityNotFound(identifier)));
    }


    public Mono<Transaction> createEntity(IncomingStatements triples) {
        return Mono.just(triples)
                .flatMap(sts -> {
                    for (Resource obj : new ArrayList<>(triples.getModel().subjects())) {

                        /* check if each node object has a valid type definition */
                        if (!triples.getModel().contains(obj, RDF.TYPE, null)) {
                            log.error("The object {} is missing a type", obj);
                            return Mono.error(new MissingType("Missing type definition for object"));
                        }

                        /* TODO: check if create of resource of given type is supported or is it delegated to connector */

                        /* Handle Ids */
                        if (obj.isBNode()) {
                            // generate a new qualified identifier if it is an anonymous node
                            triples.replaceAnonymousIdentifier(obj);

                        } else {
                            // otherwise check if id already exists in graph
                            if (graph.existsSync(obj)) {
                                return Mono.error(new EntityExistsAlready(obj));
                            }
                        }

                        /* TODO: separate into different contexts by prefix */
                    }
                    return Mono.just(sts);
                })
                .flatMap(incomingStatements -> graph.store(triples.getModel()));
    }

    /**
     * Saves a value, return an transaction
     *
     * @param id              entity id
     * @param predicatePrefix prefix of the predicate's namespace
     * @param predicateKey    key of the predicate
     * @param value           value
     * @return transaction model
     */
    public Mono<Transaction> setValue(String id, String predicatePrefix, String predicateKey, String value) {
        ValueFactory vf = this.graph.getValueFactory();
        EntityIRI entityIdentifier = EntityIRI.withDefaultNamespace(id);
        String namespace = schema.getNamespaceFor(predicatePrefix).orElseThrow().getName();

        return this.addStatement(entityIdentifier,
                EntityIRI.withDefinedNamespace(namespace, predicateKey),
                vf.createLiteral(value)
        );
    }

    /**
     * Adds a statement
     */
    public Mono<Transaction> addStatement(Resource subj, org.eclipse.rdf4j.model.IRI predicate, Value value) {
        return this.addStatement(subj, predicate, value, new Transaction());
    }

    /**
     * Adds a statement. Fails if no entity exists with the given subject
     */
    public Mono<Transaction> addStatement(Resource subj, org.eclipse.rdf4j.model.IRI predicate, Value value, Transaction transaction) {
        return this.graph.exists(subj)
                .flatMap(exists -> {
                    if (exists) return this.graph.store(subj, predicate, value, transaction);
                    else return Mono.error(new EntityNotFound(subj.stringValue()));
                });


    }



    /**
     * Adds the entity to the model and creates a connection from the entity to the new entity
     * <p>
     * If the relation is bidirectional, we should also create the inverse edge
     *
     * @param id              entity id
     * @param predicatePrefix prefix of the predicate's namespace
     * @param predicateKey    key of the predicate
     * @param linkedEntities  value
     * @return transaction model
     */
    public Mono<Transaction> link(String id, String predicatePrefix, String predicateKey, IncomingStatements linkedEntities) throws IOException {

        EntityIRI entityIdentifier = EntityIRI.withDefaultNamespace(id);
        String namespace = schema.getNamespaceFor(predicatePrefix).orElseThrow().getName();
        org.eclipse.rdf4j.model.IRI predicate = EntityIRI.withDefinedNamespace(namespace, predicateKey);

        /*
            Constraints to check:
                the incoming model can contain multiple entities, all will be linked to
                the model cannot contain the link statements itself (we are creating them)


         */

        return this.graph.type(entityIdentifier)
                .switchIfEmpty(Mono.error(new EntityNotFound(id)))
                .flatMap(type -> this.createEntity(linkedEntities))
                .flatMap(transaction -> {
                    ModelBuilder modelBuilder = new ModelBuilder();
                    transaction.listModifiedResources()
                            .forEach(value -> modelBuilder.add(entityIdentifier, predicate, value));

                    return this.graph.store(modelBuilder.build(), transaction);
                });



        // FIXME: we should separate by entities (and have them as individual transactions)


    }
}
