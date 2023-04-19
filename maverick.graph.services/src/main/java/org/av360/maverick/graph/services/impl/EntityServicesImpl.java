package org.av360.maverick.graph.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.enums.Activity;
import org.av360.maverick.graph.model.errors.requests.EntityNotFound;
import org.av360.maverick.graph.model.events.EntityCreatedEvent;
import org.av360.maverick.graph.model.events.EntityDeletedEvent;
import org.av360.maverick.graph.model.rdf.LocalIRI;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.model.vocabulary.SDO;
import org.av360.maverick.graph.services.EntityServices;
import org.av360.maverick.graph.services.QueryServices;
import org.av360.maverick.graph.services.SchemaServices;
import org.av360.maverick.graph.services.transformers.DelegatingTransformer;
import org.av360.maverick.graph.services.validators.DelegatingValidator;
import org.av360.maverick.graph.store.EntityStore;
import org.av360.maverick.graph.store.TransactionsStore;
import org.av360.maverick.graph.store.rdf.fragments.RdfEntity;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.av360.maverick.graph.store.rdf.fragments.TripleBag;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.ConstructQuery;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Slf4j(topic = "graph.srvc.entity")
@Service
public class EntityServicesImpl implements EntityServices {


    private final EntityStore entityStore;
    private final TransactionsStore trxStore;

    private final SchemaServices schemaServices;
    private final QueryServices queryServices;

    private final ApplicationEventPublisher eventPublisher;

    private DelegatingValidator validators;
    private DelegatingTransformer transformers;





    public EntityServicesImpl(EntityStore graph,
                              TransactionsStore trxStore,
                              SchemaServices schemaServices, QueryServices queryServices, ApplicationEventPublisher eventPublisher) {
        this.entityStore = graph;
        this.trxStore = trxStore;
        this.schemaServices = schemaServices;
        this.queryServices = queryServices;
        this.eventPublisher = eventPublisher;
    }




    @Override
    public Mono<RdfEntity> get(IRI entityIri, Authentication authentication, int includeNeighboursLevel) {
        return entityStore.getEntity(entityIri, authentication, includeNeighboursLevel)
                .switchIfEmpty(Mono.error(new EntityNotFound(entityIri)));
    }

    @Override
    public Flux<RdfEntity> list(Authentication authentication, int limit, int offset) {
        /*
        PREFIX sdo:    <https://schema.org/>
        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
            CONSTRUCT   {
              ?x a ?type ;
                 sdo:title ?title ;
                 skos:prefLabel ?label.
            } WHERE {
              ?x a <urn:pwid:meg:e:Individual> ;
                 a ?type .
              OPTIONAL {
                ?x sdo:title ?title .
              }
            }
         */

        Variable idVariable = SparqlBuilder.var("id");
        Variable typeVariable = SparqlBuilder.var("type");
        Variable l1 = SparqlBuilder.var("l1");
        Variable l2 = SparqlBuilder.var("l2");
        Variable l3 = SparqlBuilder.var("l3");
        Variable l4 = SparqlBuilder.var("l4");

        TriplePattern resPattern = idVariable.isA(typeVariable).andHas(SDO.TITLE, l1).andHas(RDFS.LABEL, l2).andHas(DCTERMS.TITLE, l3).andHas(SKOS.PREF_LABEL, l4);
        GraphPattern wherePattern = idVariable.isA(Local.Entities.INDIVIDUAL).andIsA(typeVariable).and(idVariable.has(RDFS.LABEL, l1).optional())
                .and(idVariable.has(DCTERMS.TITLE, l2).optional())
                .and(idVariable.has(SDO.TITLE, l3).optional())
                .and(idVariable.has(SKOS.PREF_LABEL, l4).optional());

        ConstructQuery q = Queries.CONSTRUCT(resPattern).where(wherePattern).limit(limit).offset(offset);


        final Resource[] current = {null};
        return this.queryServices.queryGraph(q, authentication)
                .bufferUntil(annotatedStatement -> {
                    if(annotatedStatement.getSubject() == current[0]) return true;
                    else {
                        current[0] = annotatedStatement.getSubject();
                        return false;
                    }
                })
                .map(statements -> {
                    RdfEntity rdfEntity = new RdfEntity(current[0]);
                    rdfEntity.getModel().addAll(statements);
                    return rdfEntity;
                });
        /*
        return this.queryServices.queryValues(query.getQueryString(), authentication)
                .map(bindings -> {
                    Value value = bindings.getValue(idVariable.getVarName());
                    if(! value.isIRI()) return;

                    RdfEntity rdfEntity = new RdfEntity((IRI) value);
                    rdfEntity.getBuilder().subject()


                })
                .filter(Value::isIRI)
                .map(value -> (IRI) value)
                .flatMap(id -> this.entityStore.getEntity(id, authentication, 0));
         */
    }
    @Override
    public Mono<RdfEntity> findByKey(String entityKey, Authentication authentication) {
        return schemaServices.resolveLocalName(entityKey)
                .flatMap(entityIdentifier -> this.get(entityIdentifier, authentication, 1));
    }

    @Override
    public Mono<RdfEntity> findByProperty(String identifier, IRI predicate, Authentication authentication) {
        Literal identifierLit = entityStore.getValueFactory().createLiteral(identifier);

        Variable idVariable = SparqlBuilder.var("id");

        SelectQuery query = Queries.SELECT(idVariable).where(
                idVariable.has(predicate, identifierLit));

        return this.entityStore.query(query.getQueryString(), authentication)
                .next()
                .map(bindings -> bindings.getValue(idVariable.getVarName()))
                .flatMap(id -> this.entityStore.getEntity((Resource) id, authentication, 1))
                .switchIfEmpty(Mono.error(new EntityNotFound(identifier)));
    }

    @Override
    public Mono<RdfEntity> find(String identifier, String property, Authentication authentication) {
        if(StringUtils.hasLength(property)) {
            return schemaServices.resolvePrefixedName(property)
                    .flatMap(propertyIri -> this.findByProperty(identifier, propertyIri, authentication));
        } else {
            return this.findByKey(identifier, authentication);
        }
    }

    @Override
    public Mono<Boolean> contains(IRI entityIri, Authentication authentication) {
        return entityStore.exists(entityIri, authentication);
    }






    public Mono<IRI> resolveAndVerify(String key, Authentication authentication) {
        LocalIRI targetIri = LocalIRI.withDefaultNamespace(key);
        return this.contains(targetIri, authentication)
                .flatMap(exists -> {
                    if(! exists) return Mono.error(new EntityNotFound(key));
                    else return Mono.just(targetIri);
                });
    }

    @Override
    public EntityStore getStore() {
        return this.entityStore;
    }


    @Override
    public Mono<RdfTransaction> remove(IRI entityIri, Authentication authentication) {
        return this.entityStore.listStatements(entityIri, null, null, authentication)
                .flatMap(statements -> this.entityStore.removeStatements(statements, new RdfTransaction()))
                .flatMap(trx -> this.entityStore.commit(trx, authentication))
                .doOnSuccess(transaction -> {
                    eventPublisher.publishEvent(new EntityDeletedEvent(transaction));
                });
    }


    @Override
    public Mono<RdfTransaction> remove(String entityKey, Authentication authentication) {
        return this.remove(LocalIRI.withDefaultNamespace(entityKey), authentication);
    }

    @Override
    public Mono<RdfTransaction> create(TripleBag triples, Map<String, String> parameters, Authentication authentication) {
        return this.prepareEntity(triples, parameters, new RdfTransaction(), authentication)
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
     * @param predicate       the qualified predicate
     * @param linkedEntities  value
     * @return transaction model
     */
    @Override
    @Deprecated
    public Mono<RdfTransaction> linkEntityTo(String id, IRI predicate, TripleBag linkedEntities, Authentication authentication) {

        LocalIRI entityIdentifier = LocalIRI.withDefaultNamespace(id);

        /*
            Constraints to check:
                the incoming model can contain multiple entities, all will be linked to
                the model cannot contain the link statements itself (we are creating them)
         */
        return this.entityStore.getEntity(entityIdentifier, authentication, 0)
                .switchIfEmpty(Mono.error(new EntityNotFound(id)))

                /* store the new entities */
                .map(entity -> new RdfTransaction().affected(entity))
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
    protected Mono<RdfTransaction> prepareEntity(TripleBag triples, Map<String, String> parameters, RdfTransaction transaction, Authentication authentication) {
        if (log.isTraceEnabled())
            log.trace("Validating and transforming {} statements for new entity. Parameters: {}", triples.streamStatements().count(), parameters.size() > 0 ? parameters : "none");

        // TODO: perform validation via sha
        // https://rdf4j.org/javadoc/3.2.0/org/eclipse/rdf4j/sail/shacl/ShaclSail.html
        return Mono.just(triples)

                .flatMap(sts -> {
                    /* validate */
                    return validators.handle(this, sts, parameters);
                })

                .flatMap(sts -> {
                    /* transform */
                    return transformers.handle(sts, parameters);

                    /* TODO: check if create of resource of given type is supported or is it delegated to connector */

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

        return schemaServices.getNamespaceFor(predicatePrefix)
                .map(ns -> LocalIRI.withDefinedNamespace(ns, predicateKey))
                .flatMap(predicate -> this.valueIsSet(entityIdentifier, predicate, authentication));
    }

    /* for testing only */
    public Mono<Boolean> entityExists(String id, Authentication authentication) {
        LocalIRI identifier = LocalIRI.withDefaultNamespace(id);

        return this.entityStore.listStatements(identifier, null, null, authentication)
                .hasElement();
    }
}
