package com.bechtle.eagl.graph.domain.services;

import com.bechtle.eagl.graph.domain.model.errors.EntityNotFound;
import com.bechtle.eagl.graph.domain.model.errors.UnknownPrefix;
import com.bechtle.eagl.graph.domain.model.extensions.EntityIRI;
import com.bechtle.eagl.graph.domain.model.wrapper.Entity;
import com.bechtle.eagl.graph.domain.model.wrapper.Incoming;
import com.bechtle.eagl.graph.domain.model.wrapper.Transaction;
import com.bechtle.eagl.graph.domain.services.handler.Transformers;
import com.bechtle.eagl.graph.domain.services.handler.Validators;
import com.bechtle.eagl.graph.repository.EntityStore;
import com.bechtle.eagl.graph.repository.SchemaStore;
import com.bechtle.eagl.graph.repository.TransactionsStore;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EntityServices {

    private final EntityStore graph;
    private final TransactionsStore trxStore;
    private final SchemaStore schema;
    private final Validators validators;
    private final Transformers transformers;

    public EntityServices(EntityStore graph,
                          TransactionsStore trxStore,
                          SchemaStore schema,
                          Validators validators,
                          Transformers transformers) {
        this.graph = graph;
        this.trxStore = trxStore;
        this.schema = schema;
        this.validators = validators;
        this.transformers = transformers;
    }

    public Mono<Entity> readEntity(String identifier) {
        return graph.get(EntityIRI.withDefaultNamespace(identifier))
                .switchIfEmpty(Mono.error(new EntityNotFound(identifier)));
    }


    public Mono<Transaction> createEntity(Incoming triples, Map<String, String> parameters) {
        return this.createEntity(triples, parameters, new Transaction())
                // FIXME: should be handled async through application event (since it is just for browsing versions)
                .flatMap(trxStore::store);

    }


    /*
        Make sure you store the transaction once you are finished
     */
    private Mono<Transaction> createEntity(Incoming triples, Map<String, String> parameters, Transaction transaction) {
        if(log.isDebugEnabled()) log.debug("(Service) {} statements incoming for creating new entity. Parameters: {}", triples.streamNamespaceAwareStatements().count(),  parameters.size() > 0 ? parameters : "none");

        // TODO: perform validation via sha
        // https://rdf4j.org/javadoc/3.2.0/org/eclipse/rdf4j/sail/shacl/ShaclSail.html

        return Mono.just(triples)
                .flatMap(sts -> {
                    /* validate */
                    return validators.delegate(sts, graph, parameters);
                })

                .flatMap(sts -> {
                    /* transform */
                    return transformers.delegate(sts, graph, parameters);

                    /* TODO: check if create of resource of given type is supported or is it delegated to connector */

                })

                .flatMap(sts ->graph.store(sts.getModel(), transaction))
                .map(trx -> {
                    return trx;
                });
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
        String namespace = schema.getNamespaceFor(predicatePrefix).orElseThrow(() -> new UnknownPrefix(predicatePrefix)).getName();

        return this.addStatement(entityIdentifier,
                EntityIRI.withDefinedNamespace(namespace, predicateKey),
                vf.createLiteral(value)
        );
    }

    /**
     * Adds a statement
     */
    public Mono<Transaction> addStatement(IRI subj, org.eclipse.rdf4j.model.IRI predicate, Value value) {
        return this.addStatement(subj, predicate, value, new Transaction())
                .flatMap(trxStore::store);
    }

    /**
     * Adds a statement. Fails if no entity exists with the given subject
     */
    public Mono<Transaction> addStatement(IRI entityIdentifier, IRI predicate, Value value, Transaction transaction) {
        return this.graph.get(entityIdentifier)
                .switchIfEmpty(Mono.error(new EntityNotFound(entityIdentifier.stringValue())))
                .flatMap(entity -> this.graph.store(entityIdentifier, predicate, value, transaction.with(entity)));
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
    public Mono<Transaction> link(String id, String predicatePrefix, String predicateKey, Incoming linkedEntities)  {

        EntityIRI entityIdentifier = EntityIRI.withDefaultNamespace(id);
        String namespace = schema.getNamespaceFor(predicatePrefix).orElseThrow(() -> new UnknownPrefix(predicatePrefix)).getName();
        IRI predicate = EntityIRI.withDefinedNamespace(namespace, predicateKey);

        /*
            Constraints to check:
                the incoming model can contain multiple entities, all will be linked to
                the model cannot contain the link statements itself (we are creating them)


         */

        return this.graph.get(entityIdentifier)
                .switchIfEmpty(Mono.error(new EntityNotFound(id)))

                /* store the new entities */
                .flatMap(entity -> this.createEntity(linkedEntities, new HashMap<>(), new Transaction().with(entity)))

                /* store the links */
                .flatMap(transaction -> {
                    ModelBuilder modelBuilder = new ModelBuilder();

                    List<Value> values = transaction.listModifiedResources();
                    transaction.listModifiedResources().forEach(value -> modelBuilder.add(entityIdentifier, predicate, value));

                    return this.graph.store(modelBuilder.build(), transaction);
                });




        // FIXME: we should separate by entities (and have them as individual transactions)


    }
}
