package com.bechtle.cougar.graph.domain.services;

import com.bechtle.cougar.graph.domain.services.handler.DelegatingTransformer;
import com.bechtle.cougar.graph.domain.services.handler.DelegatingValidator;
import com.bechtle.cougar.graph.repository.EntityStore;
import com.bechtle.cougar.graph.repository.SchemaStore;
import com.bechtle.cougar.graph.repository.TransactionsStore;
import com.bechtle.cougar.graph.domain.model.enums.Activity;
import com.bechtle.cougar.graph.domain.model.errors.EntityNotFound;
import com.bechtle.cougar.graph.domain.model.errors.UnknownPrefix;
import com.bechtle.cougar.graph.domain.model.extensions.LocalIRI;
import com.bechtle.cougar.graph.domain.model.wrapper.Entity;
import com.bechtle.cougar.graph.domain.model.wrapper.Incoming;
import com.bechtle.cougar.graph.domain.model.wrapper.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Slf4j(topic = "cougar.graph.service.entity")
@Service
public class EntityServices extends ServicesBase {

    private final EntityStore entityStore;
    private final TransactionsStore trxStore;
    private final SchemaStore schema;
    private final DelegatingValidator validators;
    private final DelegatingTransformer transformers;

    public EntityServices(EntityStore graph,
                          TransactionsStore trxStore,
                          SchemaStore schema,
                          DelegatingValidator validators,
                          DelegatingTransformer transformers) {
        this.entityStore = graph;
        this.trxStore = trxStore;
        this.schema = schema;
        this.validators = validators;
        this.transformers = transformers;
    }

    public Mono<Entity> readEntity(String identifier) {
        return ReactiveSecurityContextHolder.getContext().map(SecurityContext::getAuthentication)
                .flatMap(authentication -> entityStore.getEntity(LocalIRI.withDefaultNamespace(identifier), authentication))
                //.switchIfEmpty()        // check if param identifier has been a duplicate identifier and is stored in dc.identifier
                .switchIfEmpty(Mono.error(new EntityNotFound(identifier)));
    }


    public Mono<Transaction> createEntity(Incoming triples, Map<String, String> parameters) {
        return Mono.zip(
                this.createEntity(triples, parameters, new Transaction()),
                getAuthentication()
        ).flatMap(tpl -> entityStore.commit(tpl.getT1(), tpl.getT2()))
                .map(transaction -> {
                    return transaction;
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


        ValueFactory vf = SimpleValueFactory.getInstance();
        LocalIRI entityIdentifier = LocalIRI.withDefaultNamespace(id);
        String namespace = schema.getNamespaceFor(predicatePrefix).orElseThrow(() -> new UnknownPrefix(predicatePrefix)).getName();

        return this.setValue(entityIdentifier,
                LocalIRI.withDefinedNamespace(namespace, predicateKey),
                vf.createLiteral(value)
        );
    }

    /**
     * Adds a statement
     */
    public Mono<Transaction> setValue(IRI subj, org.eclipse.rdf4j.model.IRI predicate, Value value) {
        return this.setValue(subj, predicate, value, new Transaction());
    }

    /**
     * Adds a statement. Fails if no entity exists with the given subject
     */
    public Mono<Transaction> setValue(IRI entityIdentifier, IRI predicate, Value value, Transaction transaction) {
        return getAuthentication().flatMap(authentication ->
                        this.entityStore.getEntity(entityIdentifier, authentication)
                            .switchIfEmpty(Mono.error(new EntityNotFound(entityIdentifier.stringValue())))
                            .flatMap(entity -> this.entityStore.addStatement(entityIdentifier, predicate, value, transaction.affected(entity)))
                            .flatMap(trx -> this.entityStore.commit(trx, authentication))
                );
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
    public Mono<Transaction> link(String id, String predicatePrefix, String predicateKey, Incoming linkedEntities) {

        LocalIRI entityIdentifier = LocalIRI.withDefaultNamespace(id);
        String namespace = schema.getNamespaceFor(predicatePrefix).orElseThrow(() -> new UnknownPrefix(predicatePrefix)).getName();
        IRI predicate = LocalIRI.withDefinedNamespace(namespace, predicateKey);

        /*
            Constraints to check:
                the incoming model can contain multiple entities, all will be linked to
                the model cannot contain the link statements itself (we are creating them)


         */
        return ReactiveSecurityContextHolder.getContext().map(SecurityContext::getAuthentication)
                .flatMap(authentication -> {
                    return this.entityStore.getEntity(entityIdentifier, authentication)
                            .switchIfEmpty(Mono.error(new EntityNotFound(id)))

                            /* store the new entities */
                            .map(entity -> new Transaction().affected(entity))
                            .flatMap(transaction -> this.createEntity(linkedEntities, new HashMap<>(), transaction))

                            /* store the links */
                            .map(transaction -> {

                                transaction.listModifiedResources(Activity.INSERTED, Activity.UPDATED)
                                        .forEach(value -> {
                                            transaction.insert(entityIdentifier, predicate, value, Activity.UPDATED);
                                        });

                                return transaction;

                            })
                            .flatMap(trx -> this.entityStore.commit(trx, authentication));
                });


        // FIXME: we should separate by entities (and have them as individual transactions)
    }


    /**
      * Make sure you store the transaction once you are finished
      */
    private Mono<Transaction> createEntity(Incoming triples, Map<String, String> parameters, Transaction transaction) {
        if (log.isDebugEnabled())
            log.debug("(Service) {} statements incoming for creating new entity. Parameters: {}", triples.streamStatements().count(), parameters.size() > 0 ? parameters : "none");

        // TODO: perform validation via sha
        // https://rdf4j.org/javadoc/3.2.0/org/eclipse/rdf4j/sail/shacl/ShaclSail.html
        return Mono.just(triples)
                .flatMap(sts -> {
                    /* validate */
                    return validators.handle(this, sts, parameters);
                })

                .flatMap(sts -> {
                    /* transform */
                    return transformers.handle(this, sts, parameters);

                    /* TODO: check if create of resource of given type is supported or is it delegated to connector */

                })

                .flatMap(sts -> entityStore.insert(sts.getModel(), transaction));
    }

    public Flux<BindingSet> query(SelectQuery all) {
        return ReactiveSecurityContextHolder.getContext().map(SecurityContext::getAuthentication)
                .flatMapMany(authentication -> this.entityStore.query(all, authentication));
    }
}