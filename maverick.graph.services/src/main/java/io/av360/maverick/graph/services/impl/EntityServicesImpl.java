package io.av360.maverick.graph.services.impl;

import io.av360.maverick.graph.model.enums.Activity;
import io.av360.maverick.graph.model.errors.EntityNotFound;
import io.av360.maverick.graph.model.rdf.LocalIRI;
import io.av360.maverick.graph.model.vocabulary.Local;
import io.av360.maverick.graph.services.EntityServices;
import io.av360.maverick.graph.services.QueryServices;
import io.av360.maverick.graph.services.events.EntityCreatedEvent;
import io.av360.maverick.graph.services.events.EntityDeletedEvent;
import io.av360.maverick.graph.services.transformers.DelegatingTransformer;
import io.av360.maverick.graph.services.validators.DelegatingValidator;
import io.av360.maverick.graph.store.EntityStore;
import io.av360.maverick.graph.store.SchemaStore;
import io.av360.maverick.graph.store.TransactionsStore;
import io.av360.maverick.graph.store.rdf.models.Entity;
import io.av360.maverick.graph.store.rdf.models.TripleBag;
import io.av360.maverick.graph.store.rdf.models.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j(topic = "graph.service.entity")
@Service
public class EntityServicesImpl implements EntityServices {


    private final EntityStore entityStore;
    private final TransactionsStore trxStore;
    private final SchemaStore schema;

    private final ApplicationEventPublisher eventPublisher;

    private DelegatingValidator validators;
    private DelegatingTransformer transformers;


    private QueryServices queryServices;

    public EntityServicesImpl(EntityStore graph,
                              TransactionsStore trxStore,
                              SchemaStore schema,
                              ApplicationEventPublisher eventPublisher) {
        this.entityStore = graph;
        this.trxStore = trxStore;
        this.schema = schema;
        this.eventPublisher = eventPublisher;
    }


    @Override
    public Mono<Entity> readEntity(String identifier, Authentication authentication) {
        return entityStore.getEntity(LocalIRI.withDefaultNamespace(identifier), authentication)
                .switchIfEmpty(Mono.error(new EntityNotFound(identifier)));
    }


    @Override
    public Mono<Transaction> deleteEntity(IRI identifier, Authentication authentication) {
        return this.entityStore.listStatements(identifier, null, null, authentication)
                .flatMap(statements -> this.entityStore.removeStatements(statements, new Transaction()))
                .flatMap(trx -> this.entityStore.commit(trx, authentication))
                .doOnSuccess(transaction -> {
                    eventPublisher.publishEvent(new EntityDeletedEvent(transaction));
                });
    }


    @Override
    public Mono<Transaction> deleteEntity(String id, Authentication authentication) {
        return this.deleteEntity(LocalIRI.withDefaultNamespace(id), authentication);
    }

    @Override
    public Mono<Transaction> createEntity(TripleBag triples, Map<String, String> parameters, Authentication authentication) {
        return this.prepareEntity(triples, parameters, new Transaction(), authentication)
                .flatMap(transaction -> entityStore.commit(transaction, authentication))
                .doOnSuccess(transaction -> {
                    eventPublisher.publishEvent(new EntityCreatedEvent(transaction));
                    // TODO: throw event for every entity in payload
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
    @Override
    public Mono<Transaction> linkEntityTo(String id, String predicatePrefix, String predicateKey, TripleBag linkedEntities, Authentication authentication) {

        LocalIRI entityIdentifier = LocalIRI.withDefaultNamespace(id);
        IRI predicate = LocalIRI.withDefinedNamespace(schema.getNamespaceFor(predicatePrefix), predicateKey);

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
    protected Mono<Transaction> prepareEntity(TripleBag triples, Map<String, String> parameters, Transaction transaction, Authentication authentication) {
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
                .map(sts -> {
                    // we explicitly type the incoming object as entity (required for distinguish between entities and embedded entities in later queries)
                    Set<Resource> identifiers = new HashSet<>(sts.getModel().filter(null, RDF.TYPE, null).subjects());

                    identifiers.forEach(id ->
                            sts.getModel().add(id, RDF.TYPE, Local.Entities.TYPE)
                    );

                    return sts;

                })

                .flatMap(sts -> entityStore.insert(sts.getModel(), transaction));
    }

    @Autowired
    protected void setValidators(DelegatingValidator validators) {
        this.validators = validators;
    }

    @Autowired
    protected void setTransformers(DelegatingTransformer transformers) {
        this.transformers = transformers;
        this.transformers.registerEntityService(this);
    }


    /* for testing only */
    public Mono<Boolean> valueIsSet(IRI identifier, IRI predicate, Authentication authentication) {
        return this.entityStore.listStatements(identifier, predicate, null, authentication)
                .hasElement();
    }

    /* for testing only */
    public Mono<Boolean> valueIsSet(String id, String predicatePrefix, String predicateKey, Authentication authentication) {
        LocalIRI entityIdentifier = LocalIRI.withDefaultNamespace(id);

        return this.valueIsSet(entityIdentifier,
                LocalIRI.withDefinedNamespace(schema.getNamespaceFor(predicatePrefix), predicateKey),
                authentication
        );
    }

    /* for testing only */
    public Mono<Boolean> entityExists(String id, Authentication authentication) {
        LocalIRI identifier = LocalIRI.withDefaultNamespace(id);

        return this.entityStore.listStatements(identifier, null, null, authentication)
                .hasElement();
    }
}
