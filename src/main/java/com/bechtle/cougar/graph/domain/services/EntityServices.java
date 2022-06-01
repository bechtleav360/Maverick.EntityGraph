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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Slf4j(topic = "cougar.graph.service.entity")
@Service
public class EntityServices {

    private final EntityStore entityStore;
    private final TransactionsStore trxStore;
    private final SchemaStore schema;



    private DelegatingValidator validators;
    private DelegatingTransformer transformers;



    private QueryServices queryServices;

    public EntityServices(EntityStore graph,
                          TransactionsStore trxStore,
                          SchemaStore schema) {
        this.entityStore = graph;
        this.trxStore = trxStore;
        this.schema = schema;
    }



    public Mono<Entity> readEntity(String identifier, Authentication authentication) {
        return entityStore.getEntity(LocalIRI.withDefaultNamespace(identifier), authentication)
                .switchIfEmpty(Mono.error(new EntityNotFound(identifier)));
    }


    public Mono<Transaction> createEntity(Incoming triples, Map<String, String> parameters, Authentication authentication) {
        return this.prepareEntity(triples, parameters, new Transaction(), authentication)
                .flatMap(transaction -> entityStore.commit(transaction, authentication));

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
    public Mono<Transaction> setValue(String id, String predicatePrefix, String predicateKey, String value, Authentication authentication) {


        ValueFactory vf = SimpleValueFactory.getInstance();
        LocalIRI entityIdentifier = LocalIRI.withDefaultNamespace(id);
        String namespace = schema.getNamespaceFor(predicatePrefix).orElseThrow(() -> new UnknownPrefix(predicatePrefix)).getName();

        return this.setValue(entityIdentifier,
                LocalIRI.withDefinedNamespace(namespace, predicateKey),
                vf.createLiteral(value),
                authentication
        );
    }

    /**
     * Adds a statement
     */
    public Mono<Transaction> setValue(IRI subj, org.eclipse.rdf4j.model.IRI predicate, Value value, Authentication authentication) {
        return this.setValue(subj, predicate, value, new Transaction(), authentication);
    }

    /**
     * Adds a statement. Fails if no entity exists with the given subject
     */
    public Mono<Transaction> setValue(IRI entityIdentifier, IRI predicate, Value value, Transaction transaction, Authentication authentication) {

        return this.entityStore.getEntity(entityIdentifier, authentication)
                .switchIfEmpty(Mono.error(new EntityNotFound(entityIdentifier.stringValue())))
                .flatMap(entity -> this.entityStore.addStatement(entityIdentifier, predicate, value, transaction.affected(entity)))
                .flatMap(trx -> this.entityStore.commit(trx, authentication));
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
    public Mono<Transaction> linkEntityTo(String id, String predicatePrefix, String predicateKey, Incoming linkedEntities, Authentication authentication) {

        LocalIRI entityIdentifier = LocalIRI.withDefaultNamespace(id);
        String namespace = schema.getNamespaceFor(predicatePrefix).orElseThrow(() -> new UnknownPrefix(predicatePrefix)).getName();
        IRI predicate = LocalIRI.withDefinedNamespace(namespace, predicateKey);

        /*
            Constraints to check:
                the incoming model can contain multiple entities, all will be linked to
                the model cannot contain the link statements itself (we are creating them)


         */
        return this.entityStore.getEntity(entityIdentifier, authentication)
                .switchIfEmpty(Mono.error(new EntityNotFound(id)))

                /* store the new entities */
                .map(entity -> new Transaction().affected(entity))
                .flatMap(transaction -> this.prepareEntity(linkedEntities, new HashMap<>(), transaction, authentication))

                /* store the links */
                .map(transaction -> {

                    transaction.listModifiedResources(Activity.INSERTED, Activity.UPDATED)
                            .forEach(value -> {
                                transaction.insert(entityIdentifier, predicate, value, Activity.UPDATED);
                            });

                    return transaction;

                })
                .flatMap(trx -> this.entityStore.commit(trx, authentication));


        // FIXME: we should separate by entities (and have them as individual transactions)
    }


    /**
     * Make sure you store the transaction once you are finished
     */
    private Mono<Transaction> prepareEntity(Incoming triples, Map<String, String> parameters, Transaction transaction, Authentication authentication) {
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
                    return transformers.handle(sts, parameters, authentication);

                    /* TODO: check if create of resource of given type is supported or is it delegated to connector */

                })

                .flatMap(sts -> entityStore.insert(sts.getModel(), transaction));
    }

    @Autowired
    public void setValidators( DelegatingValidator validators) {
        this.validators = validators;
    }
    @Autowired
    public void setTransformers(DelegatingTransformer transformers) {
        this.transformers = transformers;
        this.transformers.registerEntityService(this);
    }

}
