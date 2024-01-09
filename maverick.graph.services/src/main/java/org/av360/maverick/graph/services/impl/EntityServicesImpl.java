package org.av360.maverick.graph.services.impl;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.annotations.OnRepositoryType;
import org.av360.maverick.graph.model.annotations.RequiresPrivilege;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.model.enums.Activity;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.model.errors.requests.EntityNotFound;
import org.av360.maverick.graph.model.events.EntityCreatedEvent;
import org.av360.maverick.graph.model.rdf.LocalIRI;
import org.av360.maverick.graph.model.rdf.Triples;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.model.util.ValidateReactive;
import org.av360.maverick.graph.services.EntityServices;
import org.av360.maverick.graph.services.IdentifierServices;
import org.av360.maverick.graph.services.QueryServices;
import org.av360.maverick.graph.services.SchemaServices;
import org.av360.maverick.graph.services.api.Api;
import org.av360.maverick.graph.services.preprocessors.DelegatingPreprocessor;
import org.av360.maverick.graph.store.IndividualsStore;
import org.av360.maverick.graph.store.rdf.fragments.RdfFragment;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.av360.maverick.graph.store.rdf.helpers.RdfUtils;
import org.av360.maverick.graph.store.rdf.helpers.TriplesCollector;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.ModelCollector;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFParserRegistry;
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

    private final IndividualsStore entityStore;
    private final SchemaServices schemaServices;
    private final QueryServices queryServices;
    private final IdentifierServices identifierServices;
    private final ApplicationEventPublisher eventPublisher;
    private DelegatingPreprocessor preprocessor;

    private final Api api;
    public EntityServicesImpl(IndividualsStore graph,
                              SchemaServices schemaServices, QueryServices queryServices, IdentifierServices identifierServices, ApplicationEventPublisher eventPublisher, Api api) {
        this.entityStore = graph;
        this.schemaServices = schemaServices;
        this.queryServices = queryServices;
        this.identifierServices = identifierServices;
        this.eventPublisher = eventPublisher;

        this.api = api;
    }


    @Override
    @RequiresPrivilege(Authorities.READER_VALUE)
    @OnRepositoryType(RepositoryType.ENTITIES)
    public Mono<RdfFragment> get(Resource entityIri, boolean details, int depth, SessionContext ctx) {
        return this.api.entities().getStore().asFragmentable().getFragment(entityIri, depth, details, ctx.getEnvironment())
                .switchIfEmpty(Mono.error(new EntityNotFound(entityIri)));
    }


    @Override
    @RequiresPrivilege(Authorities.READER_VALUE)
    @OnRepositoryType(RepositoryType.ENTITIES)
    public Flux<RdfFragment> list(int limit, int offset, SessionContext ctx) {
        return this.list(limit, offset, ctx, null);
    }

    @Override
    @RequiresPrivilege(Authorities.READER_VALUE)
    @OnRepositoryType(RepositoryType.ENTITIES)
    public Flux<RdfFragment> list(int limit, int offset, SessionContext ctx, String query) {
        return api.entities().find().list(limit, offset, ctx, query);
    }

    @Override
    @RequiresPrivilege(Authorities.READER_VALUE)
    @OnRepositoryType(RepositoryType.ENTITIES)
    public Mono<RdfFragment> findByKey(String entityKey, boolean details, int depth, SessionContext ctx) {
        return api.identifiers().localIdentifiers().asLocalIRI(entityKey, ctx.getEnvironment())
                .flatMap(entityIdentifier -> this.get(entityIdentifier, details, depth, ctx));
    }

    @Override
    @RequiresPrivilege(Authorities.READER_VALUE)
    @OnRepositoryType(RepositoryType.ENTITIES)
    public Mono<RdfFragment> findByProperty(String identifier, IRI predicate, boolean details, int depth, SessionContext ctx) {
        return api.entities().find().findByProperty(identifier, predicate, details, depth, ctx);
    }

    @Override
    @RequiresPrivilege(Authorities.READER_VALUE)
    @OnRepositoryType(RepositoryType.ENTITIES)
    public Mono<RdfFragment> find(String key, @Nullable String property, boolean details, int depth, SessionContext ctx) {
        if (StringUtils.hasLength(property)) {
            return this.api.identifiers().prefixes().resolvePrefixedName(property)
                    .flatMap(propertyIri -> this.findByProperty(key, propertyIri, details, depth, ctx));
        } else {
            return this.findByKey(key,  details, depth, ctx);
        }
    }

    @Override
    @RequiresPrivilege(Authorities.READER_VALUE)
    @OnRepositoryType(RepositoryType.ENTITIES)
    public Mono<Boolean> contains(IRI entityIri, SessionContext ctx) {
        return this.api.entities().select().exists(entityIri, ctx);
    }




    @Override
    @RequiresPrivilege(Authorities.MAINTAINER_VALUE)
    @OnRepositoryType(RepositoryType.ENTITIES)
    public IndividualsStore getStore(SessionContext ctx) {
        return this.entityStore;
    }

    @Override
    @RequiresPrivilege(Authorities.MAINTAINER_VALUE)
    @OnRepositoryType(RepositoryType.ENTITIES)
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
    @RequiresPrivilege(Authorities.READER_VALUE)
    @OnRepositoryType(RepositoryType.ENTITIES)
    public Mono<Long> count(SessionContext ctx) {
       return api.entities().find().count(ctx);
    }


    @Override
    @RequiresPrivilege(Authorities.MAINTAINER_VALUE)
    @OnRepositoryType(RepositoryType.ENTITIES)
    public Mono<Transaction> remove(IRI entityIri, SessionContext ctx) {
        return api.entities().updates().delete(entityIri, ctx);
    }


    @Override
    @RequiresPrivilege(Authorities.MAINTAINER_VALUE)
    @OnRepositoryType(RepositoryType.ENTITIES)
    public Mono<Transaction> remove(String entityKey, SessionContext ctx) {
        return api.identifiers().localIdentifiers().asLocalIRI(entityKey, ctx.getEnvironment())
                .flatMap(entityIdentifier -> this.remove(entityIdentifier, ctx));
    }

    @Override
    @RequiresPrivilege(Authorities.CONTRIBUTOR_VALUE)
    @OnRepositoryType(RepositoryType.ENTITIES)
    public Mono<Transaction> create(Triples triples, Map<String, String> parameters, SessionContext ctx) {
        // Mono.just(new RdfTransaction().inserts(triples.getModel()))

        return this.prepareTransactions(triples, parameters, new RdfTransaction(), ctx)
                .flatMap(transaction -> entityStore.asCommitable().commit(transaction, ctx.getEnvironment()))
                .doOnSuccess(transaction -> {
                    eventPublisher.publishEvent(new EntityCreatedEvent(transaction));
                    // TODO: throw event for every entity in payload
                });


        // return this.authorizationService.check(ctx, Authorities.CONTRIBUTOR).then(action);
    }

    /**
     * Make sure you store the transaction once you are finished
     */
    protected Mono<Transaction> prepareTransactions(Triples triples, Map<String, String> parameters, RdfTransaction transaction, SessionContext context) {

        if (log.isTraceEnabled())
            log.trace("Validating and transforming {} statements for new entity. Parameters: {}", triples.streamStatements().count(), parameters.size() > 0 ? parameters : "none");

        // TODO: perform validation via sha
        // https://rdf4j.org/javadoc/3.2.0/org/eclipse/rdf4j/sail/shacl/ShaclSail.html
        return Mono.just(triples)
                .map(Triples::getModel)
                .flatMap(model -> preprocessor.handle(model, parameters, context.getEnvironment()))
                .flatMap(model -> entityStore.asCommitable().insertStatements(model, transaction));
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
    @OnRepositoryType(RepositoryType.ENTITIES)
    public Mono<Transaction> linkEntityTo(String id, IRI predicate, Triples linkedEntities, SessionContext ctx) {


        /*
            Constraints to check:
                the incoming model can contain multiple entities, all will be linked to
                the model cannot contain the link statements itself (we are creating them)
         */
        return this.identifierServices.asLocalIRI(id, ctx.getEnvironment())
                .flatMap(iri -> {
                    return this.entityStore.asFragmentable().getFragment(iri, 0, false, ctx.getEnvironment())
                            .switchIfEmpty(Mono.error(new EntityNotFound(id)))

                            /* store the new entities */
                            .map(entity -> new RdfTransaction().affects(entity))
                            .flatMap(transaction -> this.prepareTransactions(linkedEntities, new HashMap<>(), transaction, ctx))

                            /* store the links */
                            .map(transaction -> {
                                transaction.affectedSubjects(Activity.INSERTED, Activity.UPDATED)
                                        .forEach(value -> {
                                            transaction.inserts(iri, predicate, value);
                                        });

                                return transaction;

                            })
                            .flatMap(trx -> this.entityStore.asCommitable().commit(trx, ctx.getEnvironment()));


                    // FIXME: we should separate by entities (and have them as individual transactions)
                });
    }



    @Autowired
    protected void setPreprocessor(DelegatingPreprocessor preprocessor) {
        this.preprocessor = preprocessor;
        this.preprocessor.registerEntityService(this);
        this.preprocessor.registerSchemaService(this.schemaServices);
        this.preprocessor.registerQueryService(this.queryServices);
        this.preprocessor.registerIdentifierService(this.identifierServices);
    }


    /* for testing only */
    @RequiresPrivilege(Authorities.SYSTEM_VALUE)
    @OnRepositoryType(RepositoryType.ENTITIES)
    public Mono<Boolean> valueIsSet(IRI identifier, IRI predicate, SessionContext ctx) {
        return this.entityStore.asStatementsAware().listStatements(identifier, predicate, null, ctx.getEnvironment())
                .hasElement();
    }

    /* for testing only */
    @RequiresPrivilege(Authorities.SYSTEM_VALUE)
    @OnRepositoryType(RepositoryType.ENTITIES)
    public Mono<Boolean> valueIsSet(String id, String predicatePrefix, String predicateKey, SessionContext ctx) {
        LocalIRI entityIdentifier = LocalIRI.withDefaultNamespace(id);

        return this.identifierServices.asLocalIRI(id, ctx.getEnvironment())
                .flatMap(iri ->
                        schemaServices.getNamespaceFor(predicatePrefix)
                                .map(ns -> LocalIRI.withDefinedNamespace(ns, predicateKey))
                                .flatMap(predicate -> this.valueIsSet(entityIdentifier, predicate, ctx))
                );
    }

    /* for testing only */
    @Override
    @RequiresPrivilege(Authorities.MAINTAINER_VALUE)
    @OnRepositoryType(RepositoryType.ENTITIES)
    public Mono<Model> asModel(SessionContext ctx) {
        return this.entityStore.asStatementsAware().listStatements(null, null, null, ctx.getEnvironment())
                .map(statements -> statements.stream().collect(new ModelCollector()));
    }


}
