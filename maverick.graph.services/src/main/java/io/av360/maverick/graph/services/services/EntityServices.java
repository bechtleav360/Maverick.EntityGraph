package io.av360.maverick.graph.services.services;

import io.av360.maverick.graph.model.enums.Activity;
import io.av360.maverick.graph.model.errors.EntityNotFound;
import io.av360.maverick.graph.model.errors.UnknownPrefix;
import io.av360.maverick.graph.model.rdf.LocalIRI;
import io.av360.maverick.graph.model.vocabulary.Local;
import io.av360.maverick.graph.services.services.handler.DelegatingTransformer;
import io.av360.maverick.graph.services.services.handler.DelegatingValidator;
import io.av360.maverick.graph.store.EntityStore;
import io.av360.maverick.graph.store.SchemaStore;
import io.av360.maverick.graph.store.TransactionsStore;
import io.av360.maverick.graph.store.rdf.models.Entity;
import io.av360.maverick.graph.store.rdf.models.Incoming;
import io.av360.maverick.graph.store.rdf.models.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;

@Slf4j(topic = "graph.service.entity")
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


    public Mono<Transaction> deleteEntity(IRI identifier, Authentication authentication) {
        return this.entityStore.getEntity(identifier, authentication)
                .switchIfEmpty(Mono.error(new EntityNotFound(identifier.stringValue())))
                .map(entity -> new Transaction().affected(entity))
                .flatMap(transaction -> {
                    return this.entityStore.listStatements(identifier, null, null, authentication)
                            .flatMap(statements -> this.entityStore.removeStatements(statements, transaction));

                })
                .flatMap(trx -> this.entityStore.commit(trx, authentication));

    }


    public Mono<Transaction> deleteEntity(String id, Authentication authentication) {


        LocalIRI entityIdentifier = LocalIRI.withDefaultNamespace(id);

        return this.deleteEntity(entityIdentifier, authentication);
    }


    public Mono<Boolean> valueIsSet(IRI identifier, IRI predicate, Authentication authentication) {
        return this.entityStore.listStatements(identifier, predicate, null, authentication)
                .hasElement();
    }


    public Mono<Boolean> valueIsSet(String id, String predicatePrefix, String predicateKey, Authentication authentication) {


        LocalIRI entityIdentifier = LocalIRI.withDefaultNamespace(id);
        String namespace = schema.getNamespaceFor(predicatePrefix).orElseThrow(() -> new UnknownPrefix(predicatePrefix)).getName();

        return this.valueIsSet(entityIdentifier,
                LocalIRI.withDefinedNamespace(namespace, predicateKey),
                authentication
        );
    }


    public Mono<Boolean> entityExists(String id, Authentication authentication) {
        LocalIRI identifier = LocalIRI.withDefaultNamespace(id);

        return this.entityStore.listStatements(identifier, null, null, authentication)
                .hasElement();
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
    Mono<Transaction> setValue(IRI entityIdentifier, IRI predicate, Value value, Transaction transaction, Authentication authentication) {

        return this.entityStore.getEntity(entityIdentifier, authentication)
                .switchIfEmpty(Mono.error(new EntityNotFound(entityIdentifier.stringValue())))
                .flatMap(entity -> this.entityStore.addStatement(entityIdentifier, predicate, value, transaction.affected(entity)))
                .flatMap(trx -> this.entityStore.commit(trx, authentication));
    }

    /**
     * Deletes a statement.
     */

    public Mono<Transaction> deleteValue(String id, String predicatePrefix, String predicateKey, String value, Authentication authentication) {


        ValueFactory vf = SimpleValueFactory.getInstance();
        LocalIRI entityIdentifier = LocalIRI.withDefaultNamespace(id);
        String namespace = schema.getNamespaceFor(predicatePrefix).orElseThrow(() -> new UnknownPrefix(predicatePrefix)).getName();

        return this.deleteValue(entityIdentifier,
                LocalIRI.withDefinedNamespace(namespace, predicateKey),
                vf.createLiteral(value),
                authentication
        );
    }


    /**
     * Deletes a statement. Fails if no entity exists with the given subject
     */
    Mono<Transaction> deleteValue(IRI entityIdentifier, IRI predicate, Value value,  Authentication authentication) {

        return this.entityStore.listStatements(entityIdentifier, predicate,value, authentication)
                .flatMap(statements -> this.entityStore.removeStatements(statements, new Transaction()))
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
    Mono<Transaction> prepareEntity(Incoming triples, Map<String, String> parameters, Transaction transaction, Authentication authentication) {
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


    /**
     * Reroutes a statement of an entity, e.g. <entity> <hasProperty> <falseEntity> to <entity> <hasProperty> <rightEntity>
     */
    public Mono<Transaction> relinkEntityProperty(Resource subject, IRI predicate, Value oldObject, Value newObject, Authentication authentication) {
        return this.entityStore.removeStatement(subject, predicate, oldObject, new Transaction())
                .flatMap(trx -> this.entityStore.addStatement(subject, predicate, newObject, trx))
                .flatMap(trx -> this.entityStore.commit(trx, authentication));
    }



}
