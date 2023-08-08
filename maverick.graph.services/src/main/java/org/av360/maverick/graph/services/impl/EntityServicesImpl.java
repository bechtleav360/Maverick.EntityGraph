package org.av360.maverick.graph.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.model.enums.Activity;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.model.errors.InconsistentModelException;
import org.av360.maverick.graph.model.errors.requests.EntityNotFound;
import org.av360.maverick.graph.model.events.EntityCreatedEvent;
import org.av360.maverick.graph.model.events.EntityDeletedEvent;
import org.av360.maverick.graph.model.rdf.LocalIRI;
import org.av360.maverick.graph.model.rdf.Triples;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.model.util.ValidateReactive;
import org.av360.maverick.graph.model.vocabulary.SDO;
import org.av360.maverick.graph.services.EntityServices;
import org.av360.maverick.graph.services.IdentifierServices;
import org.av360.maverick.graph.services.QueryServices;
import org.av360.maverick.graph.services.SchemaServices;
import org.av360.maverick.graph.services.config.RequiresPrivilege;
import org.av360.maverick.graph.services.transformers.DelegatingTransformer;
import org.av360.maverick.graph.services.validators.DelegatingValidator;
import org.av360.maverick.graph.store.EntityStore;
import org.av360.maverick.graph.store.rdf.fragments.RdfEntity;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.av360.maverick.graph.store.rdf.helpers.BindingsAccessor;
import org.av360.maverick.graph.store.rdf.helpers.RdfUtils;
import org.av360.maverick.graph.store.rdf.helpers.TriplesCollector;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFParserRegistry;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Slf4j(topic = "graph.srvc.entity")
@Service
public class EntityServicesImpl implements EntityServices {

    private final EntityStore entityStore;
    private final SchemaServices schemaServices;
    private final QueryServices queryServices;
    private final IdentifierServices identifierServices;
    private final ApplicationEventPublisher eventPublisher;
    private DelegatingValidator validators;
    private DelegatingTransformer transformers;

    public EntityServicesImpl(EntityStore graph,
                              SchemaServices schemaServices, QueryServices queryServices, IdentifierServices identifierServices, ApplicationEventPublisher eventPublisher) {
        this.entityStore = graph;
        this.schemaServices = schemaServices;
        this.queryServices = queryServices;
        this.identifierServices = identifierServices;
        this.eventPublisher = eventPublisher;

    }


    @Override
    @RequiresPrivilege(Authorities.READER_VALUE)
    public Mono<RdfEntity> get(IRI entityIri, int includeNeighboursLevel, SessionContext ctx) {
        return entityStore.getFragment(entityIri, includeNeighboursLevel, ctx.getEnvironment())
                .switchIfEmpty(Mono.error(new EntityNotFound(entityIri)));
    }


    @Override
    @RequiresPrivilege(Authorities.READER_VALUE)
    public Flux<RdfEntity> list(int limit, int offset, SessionContext ctx) {
        return this.list(limit, offset, ctx, null);
    }

    @Override
    @RequiresPrivilege(Authorities.READER_VALUE)
    public Flux<RdfEntity> list(int limit, int offset, SessionContext ctx, String query) {

        if(! StringUtils.hasLength(query)) {
            query = """
                        
                    PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                    PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
                    PREFIX sdo: <https://schema.org/>
                    PREFIX dcterms: <http://purl.org/dc/terms/>

                    SELECT ?id ?sct ?dct ?rdt ?skt (GROUP_CONCAT(DISTINCT ?type; SEPARATOR=",") AS ?types)
                    WHERE
                    {
                      {
                        SELECT ?id WHERE {
                          ?id a <urn:pwid:meg:e:Individual> .
                        }
                        LIMIT $limit
                        OFFSET $offset
                      }
                      OPTIONAL { ?id sdo:title ?sct }.
                      OPTIONAL { ?id dcterms:title ?dct }.
                      OPTIONAL { ?id rdfs:label ?rdt }.
                      OPTIONAL { ?id skos:prefLabel ?skt }.
                      ?id a ?type .
                    }
                    GROUP BY ?id  ?sct ?dct ?rdt ?skt
                """;
        }
        query = query.replace("$limit", limit + "").replace("$offset", offset + "");

        return this.queryServices.queryValuesTrusted(query, RepositoryType.ENTITIES, ctx)
                .map(BindingsAccessor::new)
                .flatMap(bnd -> {
                    try {
                        Resource resource = bnd.asIRI("id");

                        ModelBuilder builder = new ModelBuilder();
                        builder.subject(resource);
                        bnd.asSet("types").stream()
                                .map(typeString -> SimpleValueFactory.getInstance().createIRI(typeString))
                                .forEach(typeIRI -> builder.add(RDF.TYPE, typeIRI));
                        bnd.findValue("sct").ifPresent(val -> {
                            builder.add(SDO.TITLE, val);
                            builder.setNamespace(SDO.NS);
                        });
                        bnd.findValue("rdt").ifPresent(val -> {
                            builder.add(RDFS.LABEL, val);
                            builder.setNamespace(RDFS.NS);
                        });
                        bnd.findValue("dct").ifPresent(val -> {
                            builder.add(DCTERMS.TITLE, val);
                            builder.setNamespace(DCTERMS.NS);
                        });
                        bnd.findValue("skt").ifPresent(val -> {
                            builder.add(SKOS.PREF_LABEL, val);
                            builder.setNamespace(SKOS.NS);
                        });

                        return Mono.just(new RdfEntity(resource, builder.build()));
                    } catch (InconsistentModelException e) {
                        return Mono.error(e);
                    }
                });
    }

    @Override
    @RequiresPrivilege(Authorities.READER_VALUE)
    public Mono<RdfEntity> findByKey(String entityKey, SessionContext ctx) {
        return identifierServices.asIRI(entityKey, ctx.getEnvironment())
                .flatMap(entityIdentifier -> this.get(entityIdentifier, 1, ctx));
    }

    @Override
    @RequiresPrivilege(Authorities.READER_VALUE)
    public Mono<RdfEntity> findByProperty(String identifier, IRI predicate, SessionContext ctx) {
        Literal identifierLit = entityStore.getValueFactory().createLiteral(identifier);

        Variable idVariable = SparqlBuilder.var("id");

        SelectQuery query = Queries.SELECT(idVariable).where(
                idVariable.has(predicate, identifierLit));

        return this.entityStore.query(query.getQueryString(), ctx.getEnvironment())
                .next()
                .map(bindings -> bindings.getValue(idVariable.getVarName()))
                .flatMap(id -> this.entityStore.getFragment((Resource) id, 1, ctx.getEnvironment()))
                .switchIfEmpty(Mono.error(new EntityNotFound(identifier)));
    }

    @Override
    @RequiresPrivilege(Authorities.READER_VALUE)
    public Mono<RdfEntity> find(String identifier, String property, SessionContext ctx) {
        if (StringUtils.hasLength(property)) {
            return schemaServices.resolvePrefixedName(property)
                    .flatMap(propertyIri -> this.findByProperty(identifier, propertyIri, ctx));
        } else {
            return this.findByKey(identifier, ctx);
        }
    }

    @Override
    @RequiresPrivilege(Authorities.READER_VALUE)
    public Mono<Boolean> contains(IRI entityIri, SessionContext ctx) {
        return entityStore.exists(entityIri, ctx.getEnvironment());
    }


    @RequiresPrivilege(Authorities.READER_VALUE)
    public Mono<IRI> resolveAndVerify(String key, SessionContext ctx) {
        return this.identifierServices.asIRI(key, ctx.getEnvironment())
                .filterWhen(iri -> this.contains(iri, ctx))
                .switchIfEmpty(Mono.error(new EntityNotFound(key)))
                .doOnSuccess(res -> log.trace("Resolved entity key {} to qualified identifier {}", key, res.stringValue()));
        /*
                .flatMap(targetIri ->
                        this.contains(targetIri, ctx)
                                .flatMap(exists -> {
                                    if (!exists) return Mono.error(new EntityNotFound(key));
                                    else return Mono.just(targetIri);
                                }).doOnSuccess(res -> log.trace("Resolved entity key {} to qualified identifier {}", key, res.stringValue()))
                ); */

    }

    @Override
    @RequiresPrivilege(Authorities.MAINTAINER_VALUE)
    public EntityStore getStore(SessionContext ctx) {
        return this.entityStore;
    }

    @Override
    @RequiresPrivilege(Authorities.MAINTAINER_VALUE)
    public Mono<Transaction> importFile(org.springframework.core.io.Resource resource, RDFFormat format, SessionContext context) {
        Flux<DataBuffer> publisher = ValidateReactive.notNull(resource)
                .then(ValidateReactive.isTrue(resource.exists(), "Resource does not exist: "+resource.toString()))
                .thenMany(DataBufferUtils.read(resource, new DefaultDataBufferFactory(), 1024));


        return DataBufferUtils.join(publisher)
                .flatMap(dataBuffer -> {
                    RDFParser parser = RDFParserRegistry.getInstance().get(format).orElseThrow().getParser();
                    TriplesCollector handler = RdfUtils.getTriplesCollector();

                    try (InputStream is = dataBuffer.asInputStream(true)) {
                        parser.setRDFHandler(handler);
                        parser.parse(is);
                        return Mono.just(handler.getTriples());
                    } catch (Exception e) {
                        return Mono.error(e);
                    }
                })
                .flatMap(triples -> this.create(triples, Map.of(), context))
                .doOnSubscribe(s -> log.info("Importing file '{}'", resource.getFilename()))
                .doOnError(s -> log.error("Failed to import file '{}'", resource.getFilename()));
    }

    @Override
    @RequiresPrivilege(Authorities.MAINTAINER_VALUE     )
    public Mono<Model> getModel(SessionContext ctx) {
        return this.entityStore.getModel(ctx.getEnvironment());
    }


    @Override
    @RequiresPrivilege(Authorities.MAINTAINER_VALUE)
    public Mono<Transaction> remove(IRI entityIri, SessionContext ctx) {
        return this.entityStore.listStatements(entityIri, null, null, ctx.getEnvironment())
                .flatMap(statements -> this.entityStore.removeStatements(statements, new RdfTransaction()))
                .flatMap(trx -> this.entityStore.commit(trx, ctx.getEnvironment()))
                .doOnSuccess(transaction -> {
                    eventPublisher.publishEvent(new EntityDeletedEvent(transaction));
                });
    }


    @Override
    @RequiresPrivilege(Authorities.MAINTAINER_VALUE)
    public Mono<Transaction> remove(String entityKey, SessionContext ctx) {
        return this.identifierServices.asIRI(entityKey, ctx.getEnvironment()).flatMap(iri -> this.remove(iri, ctx));
    }

    @Override
    @RequiresPrivilege(Authorities.CONTRIBUTOR_VALUE)
    public Mono<Transaction> create(Triples triples, Map<String, String> parameters, SessionContext ctx) {

        // Mono.just(new RdfTransaction().inserts(triples.getModel()))

        return this.prepareEntity(triples, parameters, new RdfTransaction(), ctx)
                .flatMap(transaction -> entityStore.commit(transaction, ctx.getEnvironment()))
                .doOnSuccess(transaction -> {
                    eventPublisher.publishEvent(new EntityCreatedEvent(transaction));
                    // TODO: throw event for every entity in payload
                });


        // return this.authorizationService.check(ctx, Authorities.CONTRIBUTOR).then(action);
    }


    /**
     * Adds the entity to the model and creates a connection from the entity to the new entity
     * <p>
     * If the relation is bidirectional, we should also create the inverse edge
     *
     * @param id             entity id
     * @param predicate      the qualified predicate
     * @param linkedEntities value
     * @return transaction model
     */
    @Override
    @Deprecated
    @RequiresPrivilege(Authorities.CONTRIBUTOR_VALUE)
    public Mono<Transaction> linkEntityTo(String id, IRI predicate, Triples linkedEntities, SessionContext ctx) {


        /*
            Constraints to check:
                the incoming model can contain multiple entities, all will be linked to
                the model cannot contain the link statements itself (we are creating them)
         */
        return this.identifierServices.asIRI(id, ctx.getEnvironment())
                .flatMap(iri -> {
                    return this.entityStore.getFragment(iri, 0, ctx.getEnvironment())
                            .switchIfEmpty(Mono.error(new EntityNotFound(id)))

                            /* store the new entities */
                            .map(entity -> new RdfTransaction().affects(entity))
                            .flatMap(transaction -> this.prepareEntity(linkedEntities, new HashMap<>(), transaction, ctx))

                            /* store the links */
                            .map(transaction -> {
                                transaction.affectedSubjects(Activity.INSERTED, Activity.UPDATED)
                                        .forEach(value -> {
                                            transaction.inserts(iri, predicate, value);
                                        });

                                return transaction;

                            })
                            .flatMap(trx -> this.entityStore.commit(trx, ctx.getEnvironment()));


                    // FIXME: we should separate by entities (and have them as individual transactions)
                });
    }

    /**
     * Make sure you store the transaction once you are finished
     */
    protected Mono<Transaction> prepareEntity(Triples triples, Map<String, String> parameters, RdfTransaction transaction, SessionContext context) {
        if (log.isTraceEnabled())
            log.trace("Validating and transforming {} statements for new entity. Parameters: {}", triples.streamStatements().count(), parameters.size() > 0 ? parameters : "none");

        // TODO: perform validation via sha
        // https://rdf4j.org/javadoc/3.2.0/org/eclipse/rdf4j/sail/shacl/ShaclSail.html
        return Mono.just(triples)
                .map(Triples::getModel)
                .flatMap(model -> {
                    /* validate */
                    return validators.handle(this, model, parameters);
                })

                .flatMap(model -> {
                    /* transform */
                    return transformers.handle(model, parameters, context.getEnvironment());

                    /* TODO: check if create of resource of given type is supported or is it delegated to connector */

                })

                .flatMap(model -> entityStore.insertModel(model, transaction));

    }

    @Autowired
    protected void setValidators(DelegatingValidator validators) {
        this.validators = validators;
    }

    @Autowired
    protected void setTransformers(DelegatingTransformer transformers) {
        this.transformers = transformers;
        this.transformers.registerEntityService(this);
        this.transformers.registerSchemaService(this.schemaServices);
        this.transformers.registerQueryService(this.queryServices);
    }


    /* for testing only */
    @RequiresPrivilege(Authorities.SYSTEM_VALUE)
    public Mono<Boolean> valueIsSet(IRI identifier, IRI predicate, SessionContext ctx) {
        return this.entityStore.listStatements(identifier, predicate, null, ctx.getEnvironment())
                .hasElement();
    }

    /* for testing only */
    @RequiresPrivilege(Authorities.SYSTEM_VALUE)
    public Mono<Boolean> valueIsSet(String id, String predicatePrefix, String predicateKey, SessionContext ctx) {
        LocalIRI entityIdentifier = LocalIRI.withDefaultNamespace(id);

        return this.identifierServices.asIRI(id, ctx.getEnvironment())
                .flatMap(iri ->
                        schemaServices.getNamespaceFor(predicatePrefix)
                                .map(ns -> LocalIRI.withDefinedNamespace(ns, predicateKey))
                                .flatMap(predicate -> this.valueIsSet(entityIdentifier, predicate, ctx))
                );

    }


}
