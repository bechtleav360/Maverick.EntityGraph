package org.av360.maverick.graph.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.model.errors.requests.EntityNotFound;
import org.av360.maverick.graph.model.errors.requests.InvalidEntityUpdate;
import org.av360.maverick.graph.model.events.LinkRemovedEvent;
import org.av360.maverick.graph.model.events.ValueInsertedEvent;
import org.av360.maverick.graph.model.events.ValueRemovedEvent;
import org.av360.maverick.graph.model.events.ValueReplacedEvent;
import org.av360.maverick.graph.model.identifier.ChecksumGenerator;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.services.EntityServices;
import org.av360.maverick.graph.services.IdentifierServices;
import org.av360.maverick.graph.services.SchemaServices;
import org.av360.maverick.graph.services.ValueServices;
import org.av360.maverick.graph.services.config.RequiresPrivilege;
import org.av360.maverick.graph.store.SchemaStore;
import org.av360.maverick.graph.store.rdf.fragments.RdfEntity;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.av360.maverick.graph.store.rdf.fragments.TripleModel;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Statements;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.LanguageHandler;
import org.eclipse.rdf4j.rio.LanguageHandlerRegistry;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;

@Slf4j(topic = "graph.srvc.values")
@Service
public class ValueServicesImpl implements ValueServices {


    private final ApplicationEventPublisher eventPublisher;

    private final SchemaServices schemaServices;

    private final EntityServices entityServices;

    private final IdentifierServices identifierServices;
    public ValueServicesImpl(SchemaStore schemaStore,
                             ApplicationEventPublisher eventPublisher, SchemaServices schemaServices, EntityServices entityServices, IdentifierServices identifierServices) {
        this.eventPublisher = eventPublisher;
        this.schemaServices = schemaServices;
        this.entityServices = entityServices;
        this.identifierServices = identifierServices;
    }


    @Override
    @RequiresPrivilege(Authorities.CONTRIBUTOR_VALUE)
    public Mono<Transaction> insertValue(String entityKey, String prefixedPoperty, String value, String languageTag, @Nullable Boolean replace, SessionContext ctx) {

        return Mono.zip(
                        this.entityServices.resolveAndVerify(entityKey, ctx),
                        this.schemaServices.resolvePrefixedName(prefixedPoperty),
                        this.normalizeValue(value, languageTag)
                )
                .switchIfEmpty(Mono.error(new InvalidEntityUpdate(entityKey, "Failed to insert value")))
                .flatMap(triple -> this.insertValue(triple.getT1(), triple.getT2(), triple.getT3(), Objects.isNull(replace) ? Boolean.FALSE : replace, ctx));
    }


    /**
     * Deletes a value with a new transaction.  Fails if no entity exists with the given subject
     */
    @Override
    @RequiresPrivilege(Authorities.CONTRIBUTOR_VALUE)
    public Mono<Transaction> removeLiteral(String entityKey, String predicate, String lang, SessionContext ctx) {
        return Mono.zip(
                        this.entityServices.resolveAndVerify(entityKey, ctx),
                        this.schemaServices.resolvePrefixedName(predicate)
                )
                .switchIfEmpty(Mono.error(new InvalidEntityUpdate(entityKey, "Failed to remove literal")))
                .flatMap(tuple -> this.removeValue(tuple.getT1(), tuple.getT2(), lang, ctx));
    }

    @Override
    @RequiresPrivilege(Authorities.CONTRIBUTOR_VALUE)
    public Mono<Transaction> insertLink(String entityKey, String prefixedKey, String targetKey,  @Nullable Boolean replace,  SessionContext ctx) {
        return Mono.zip(
                        entityServices.resolveAndVerify(entityKey, ctx),
                        entityServices.resolveAndVerify(targetKey, ctx),
                        this.schemaServices.resolvePrefixedName(prefixedKey)

                )
                .switchIfEmpty(Mono.error(new InvalidEntityUpdate(entityKey, "Failed to insert link")))
                .flatMap(triple ->
                        this.insertValue(triple.getT1(), triple.getT3(), triple.getT2(), !Objects.isNull(replace) && replace, ctx)
                );
    }


    @Override
    @RequiresPrivilege(Authorities.CONTRIBUTOR_VALUE)
    public Mono<Transaction> insertValue(IRI entityIdentifier, IRI predicate, Value value, @Nullable Boolean replace, SessionContext ctx) {
        return this.insertStatement(entityIdentifier, predicate, value, new RdfTransaction(), !Objects.isNull(replace) && replace, ctx)
                .doOnSuccess(trx -> {
                    eventPublisher.publishEvent(new ValueInsertedEvent(trx));
                });
    }

    @Override
    public Mono<Transaction> insertDetail(String entityKey, String prefixedValueKey, String prefixedDetailKey, String value, SessionContext ctx) {
        // find triple statement with id - prefixed value key -
        return Mono.zip(
                        this.schemaServices.resolveLocalName(entityKey).flatMap(entityIdentifier -> this.entityServices.get(entityIdentifier, 0, ctx)),
                        this.schemaServices.resolvePrefixedName(prefixedValueKey),
                        this.schemaServices.resolvePrefixedName(prefixedDetailKey)
                ).flatMap(tuple -> {
                    RdfEntity t1 = tuple.getT1();
                    IRI predicate = tuple.getT2();
                    IRI annotationProperty = tuple.getT3();

                    Optional<Value> distinctValue = t1.findDistinctValue(t1.getIdentifier(), predicate);

                    if (distinctValue.isEmpty()) {
                        return Mono.error(new InvalidEntityUpdate(t1.getIdentifier(), "No value exists for predicate %s".formatted(predicate)));
                    }

                    Triple triple = Values.triple(t1.getIdentifier(), predicate, distinctValue.get());
                    Statement annotationStatement = Statements.statement(triple, annotationProperty, Values.literal(value), null);
                    return Mono.just(annotationStatement);

                })
                .flatMap(statement -> this.entityServices.getStore(ctx).addStatement(statement, new RdfTransaction()))
                .flatMap(trx -> this.entityServices.getStore(ctx).commit(trx, ctx.getEnvironment()))
                .switchIfEmpty(Mono.just(new RdfTransaction()));

    }

    @Override
    @RequiresPrivilege(Authorities.CONTRIBUTOR_VALUE)
    public Mono<Transaction> insertEmbedded(IRI entityIdentifier, IRI predicate, Resource embeddedNode, Set<Statement> embedded, SessionContext ctx) {
        return this.insertStatements(entityIdentifier, predicate, embeddedNode, embedded, new RdfTransaction(), ctx)
                .doOnSuccess(trx -> {
                    eventPublisher.publishEvent(new ValueInsertedEvent(trx));
                });
    }


    @Override
    @RequiresPrivilege(Authorities.CONTRIBUTOR_VALUE)
    public Mono<Transaction> removeValue(IRI entityIdentifier, IRI predicate, String lang, SessionContext ctx) {
        return this.removeValueStatement(entityIdentifier, predicate, lang, new RdfTransaction(), ctx)
                .doOnSuccess(trx -> {
                    eventPublisher.publishEvent(new ValueRemovedEvent(trx));
                });
    }


    @Override
    @RequiresPrivilege(Authorities.CONTRIBUTOR_VALUE)
    public Mono<Transaction> removeLink(String entityKey, String prefixedProperty, String targetKey, SessionContext ctx) {
        return Mono.zip(
                entityServices.resolveAndVerify(entityKey, ctx),
                entityServices.resolveAndVerify(targetKey, ctx),
                this.schemaServices.resolvePrefixedName(prefixedProperty)

        ).flatMap(triple ->
                this.removeLinkStatement(triple.getT1(), triple.getT3(), triple.getT2(), new RdfTransaction(), ctx)
        ).doOnSuccess(trx -> {
            eventPublisher.publishEvent(new LinkRemovedEvent(trx));
        }).doOnError(error -> log.error("Failed to remove link due to reason: {}", error.getMessage()));
    }


    /**
     * Reroutes a statement of an entity, e.g. <entity> <hasProperty> <falseEntity> to <entity> <hasProperty> <rightEntity>.
     * <p>
     * Has to be part of one transaction (one commit call)
     */
    @Override
    @RequiresPrivilege(Authorities.CONTRIBUTOR_VALUE)
    public Mono<Transaction> replace(IRI entityIdentifier, IRI predicate, Value oldValue, Value newValue, SessionContext ctx) {
        return this.entityServices.getStore(ctx).removeStatement(entityIdentifier, predicate, oldValue, new RdfTransaction())
                .flatMap(trx -> this.entityServices.getStore(ctx).addStatement(entityIdentifier, predicate, newValue, trx))
                .flatMap(trx -> this.entityServices.getStore(ctx).commit(trx, ctx.getEnvironment()))
                .doOnSuccess(trx -> {
                    eventPublisher.publishEvent(new ValueReplacedEvent(trx));
                });
    }

    @Override
    @RequiresPrivilege(Authorities.READER_VALUE)
    public Mono<RdfEntity> listLinks(String entityKey, String prefixedPoperty, SessionContext ctx) {
        return Mono.zip(
                this.schemaServices.resolveLocalName(entityKey)
                        .flatMap(entityIdentifier -> this.entityServices.get(entityIdentifier, 0, ctx)),
                this.schemaServices.resolvePrefixedName(prefixedPoperty)
        ).map(pair -> {
            RdfEntity entity = pair.getT1();
            IRI property = pair.getT2();

            entity.reduce((st) -> {
                boolean isTypeDefinition = st.getSubject().equals(entity.getIdentifier()) && st.getPredicate().equals(RDF.TYPE);
                boolean isProperty = st.getPredicate().equals(property);
                return isTypeDefinition || isProperty;
            });

            return entity;
        });
    }

    @Override
    @RequiresPrivilege(Authorities.READER_VALUE)
    public Mono<TripleModel> listValues(String entityKey, String prefixedPoperty, SessionContext ctx) {
        return Mono.zip(
                this.identifierServices.asIRI(entityKey, ctx.getEnvironment())
                        .flatMap(entityIdentifier -> this.entityServices.get(entityIdentifier, 0, ctx)),
                this.schemaServices.resolvePrefixedName(prefixedPoperty)
        ).map(pair -> {
            RdfEntity entity = pair.getT1();
            IRI property = pair.getT2();
            entity.reduce(st -> st.getPredicate().equals(property));
            return entity;
        }).map(entity -> {
            new HashSet<>(entity.getModel()).forEach(statement -> {
                Triple triple = Values.triple(statement);
                String hash = ChecksumGenerator.generateChecksum(statement.getObject().stringValue(), 8, 'o');
                entity.getModel().add(triple, DCTERMS.LCSH, Values.literal(hash));

            });


            return entity;
        });
    }


    private Mono<Transaction> removeLinkStatement(IRI entityIdentifier, IRI predicate, IRI targetIdentifier, Transaction transaction, SessionContext ctx) {
        return this.entityServices.getStore(ctx).listStatements(entityIdentifier, predicate, targetIdentifier, ctx.getEnvironment())
                .flatMap(statements -> this.entityServices.getStore(ctx).removeStatements(statements, transaction))
                .flatMap(trx -> this.entityServices.getStore(ctx).commit(trx, ctx.getEnvironment()));

    }


    /**
     * Deletes a value with a new transaction. Fails if no entity exists with the given subject
     */
    private Mono<Transaction> removeValueStatement(IRI entityIdentifier, IRI predicate, @Nullable String languageTag, Transaction transaction, SessionContext ctx) {
        return this.entityServices.getStore(ctx).listStatements(entityIdentifier, predicate, null, ctx.getEnvironment())
                .flatMap(statements -> {
                    if (statements.size() > 1) {
                        List<Statement> statementsToRemove = new ArrayList<>();
                        if (StringUtils.isEmpty(languageTag)) {
                            log.error("Failed to identify unique statement for predicate {} to remove for entity {}.", predicate.getLocalName(), entityIdentifier.getLocalName());
                            statements.forEach(st -> log.trace("Candidate: {} - {} ", st.getPredicate(), st.getObject()));
                            return Mono.error(new InvalidEntityUpdate(entityIdentifier, "Multiple values for given predicate detected, but no language tag in request."));
                        }
                        for (Statement st : statements) {
                            Value object = st.getObject();
                            if (object.isBNode()) {
                                log.warn("Found a link to an anonymous node. Purge it from repository.");
                                statementsToRemove.add(st);
                            } else if (object.isIRI()) {
                                return Mono.error(new InvalidEntityUpdate(entityIdentifier, "Invalid to remove links via the values api."));
                            } else if (object.isLiteral()) {
                                Literal currentLiteral = (Literal) object;
                                if (StringUtils.equals(currentLiteral.getLanguage().orElse("invalid"), languageTag)) {
                                    statementsToRemove.add(st);
                                }
                            }
                        }


                        return this.entityServices.getStore(ctx).removeStatements(statementsToRemove, transaction);
                    } else {
                        return this.entityServices.getStore(ctx).removeStatements(statements, transaction);
                    }

                })
                .flatMap(trx -> this.entityServices.getStore(ctx).commit(trx, ctx.getEnvironment()));
    }

    private Mono<Transaction> insertStatements(IRI entityIdentifier, IRI predicate, Resource embeddedNode, Set<Statement> embedded, Transaction transaction, SessionContext ctx) {
        return this.entityServices.get(entityIdentifier, ctx)
                .switchIfEmpty(Mono.error(new EntityNotFound(entityIdentifier.stringValue())))
                .map(entity -> Pair.of(entity, transaction.affects(entity.getModel())))
                .filter(pair -> !embeddedNode.isBNode())
                .switchIfEmpty(Mono.error(new InvalidEntityUpdate(entityIdentifier, "Trying to link to shared node as anonymous node.")))
                .doOnNext(pair -> {
                    Statement statement = SimpleValueFactory.getInstance().createStatement(entityIdentifier, predicate, embeddedNode);
                    embedded.add(statement);
                })
                .flatMap(pair -> this.entityServices.getStore(ctx).insertModel(new LinkedHashModel(embedded), pair.getRight()))
                .flatMap(trx -> this.entityServices.getStore(ctx).commit(trx, ctx.getEnvironment()))
                .switchIfEmpty(Mono.just(transaction));

    }

    private Mono<Transaction> insertStatement(IRI entityIdentifier, IRI predicate, Value value, Transaction transaction, boolean replace,  SessionContext ctx) {

        Triple triple = SimpleValueFactory.getInstance().createTriple(entityIdentifier, predicate, value);

        return this.entityServices.get(entityIdentifier, ctx)
                .switchIfEmpty(Mono.error(new EntityNotFound(entityIdentifier.stringValue())))
                .doOnNext(entity -> transaction.affects(entity.getModel()))
                .flatMap(entity -> {

                    // linking to bnodes is forbidden
                    if (triple.getObject().isBNode()) {
                        log.trace("Insert link for {} to anonymous node is forbidden.", entityIdentifier);
                        return Mono.error(new InvalidEntityUpdate(entityIdentifier, "Trying to link to anonymous node."));
                    } else if(triple.getObject().isIRI()) {
                        return this.buildTransactionForIRIStatement(triple, entity, transaction, replace, ctx);
                    } else if(triple.getObject().isLiteral()) {
                        return this.buildTransactionForLiteralStatement(triple, entity, transaction, replace, ctx);
                    } else return Mono.empty();

                })

                .flatMap(pair -> this.entityServices.getStore(ctx).addStatement(entityIdentifier, predicate, value, transaction))
                .flatMap(trx -> this.entityServices.getStore(ctx).commit(trx, ctx.getEnvironment()))
                .switchIfEmpty(Mono.just(transaction));

    }

    private Mono<Transaction> buildTransactionForIRIStatement(Triple statement, RdfEntity entity, Transaction transaction, boolean replace, SessionContext ctx) {
        // check if entity already has this statement. If yes, we do nothing
        if (statement.getObject().isIRI() && entity.hasStatement(statement) && ! replace) {
            log.trace("Entity {} already has a link '{}' for predicate '{}', ignoring update.", entity.getIdentifier(), statement.getObject(), statement.getPredicate());
            return Mono.empty();
        } else {
            return this.entityServices.getStore(ctx).addStatement(statement, transaction);
        }
    }


    private Mono<Transaction> buildTransactionForLiteralStatement(Triple triple, RdfEntity entity, Transaction transaction, boolean replace, SessionContext ctx) {
        if (triple.getObject().isLiteral() && entity.hasStatement(triple.getSubject(), triple.getPredicate(), null) && replace) {
            log.trace("Entity {} already has a value for predicate '{}'.", entity.getIdentifier(), triple.getPredicate());
            Literal updateValue = (Literal) triple.getObject();

            try {
                for (Statement statement : entity.listStatements(entity.getIdentifier(), triple.getPredicate(), null)) {
                    if (!statement.getObject().isLiteral())
                        throw new InvalidEntityUpdate(entity.getIdentifier(), "Replacing an existing link to another entity with a value is not allowed. ");

                    Literal currentValue = (Literal) statement.getObject();
                    if (updateValue.getLanguage().isPresent() && currentValue.getLanguage().isPresent()) {
                        // entity already has a value for this predicate. It has a language tag. If another value with the same language tag exists, we remove it.
                        if (StringUtils.equals(currentValue.getLanguage().get(), updateValue.getLanguage().get())) {
                            this.entityServices.getStore(ctx).removeStatement(statement, transaction);
                        }
                    } else {
                        // entity already has a value for this predicate. It has no language tag. If an existing value has a language tag, we throw an error. If not, we remove it.
                        if (currentValue.getLanguage().isPresent())
                            throw new InvalidEntityUpdate(entity.getIdentifier(), "This value already exists with a language tag within this entity. Please add the tag.");

                        this.entityServices.getStore(ctx).removeStatement(statement, transaction);
                    }

                }
                return Mono.just(transaction);

            } catch (InvalidEntityUpdate e) {
                return Mono.error(e);
            }

        } else return Mono.just(transaction);

    }


    /**
     * Extracts the language tag.
     * Rules:
     * - tag in request parameter > tag in value
     * - default lang tag is "en"
     * - if one is invalid, we take the other (and write a warning into the logs)
     *
     * @param value    the literal value as string
     * @param paramTag the request parameter for the language tag, can be null
     * @return the identified language tag
     */
    private Mono<Value> extractLanguageTag(String value, @Nullable String paramTag) {
        return Mono.create(sink -> {
            LanguageHandler languageHandler = LanguageHandlerRegistry.getInstance().get(LanguageHandler.BCP47).orElseThrow();
            SimpleValueFactory svf = SimpleValueFactory.getInstance();

            String valueTag = value.matches(".*@\\w\\w-?[\\w\\d-]*$") ? value.substring(value.lastIndexOf('@') + 1) : "";
            String strippedValue = StringUtils.isNotBlank(valueTag) ? value.substring(0, value.lastIndexOf('@')) : value;

            if (StringUtils.isNotBlank(paramTag)) {
                if (languageHandler.isRecognizedLanguage(paramTag) && languageHandler.verifyLanguage(value, paramTag)) {
                    sink.success(languageHandler.normalizeLanguage(strippedValue, paramTag, svf));
                } else {
                    sink.error(new IOException("Invalid language tag in parameter: " + paramTag));
                }
            } else if (StringUtils.isNotBlank(valueTag)) {
                if (languageHandler.isRecognizedLanguage(valueTag) && languageHandler.verifyLanguage(value, valueTag)) {
                    sink.success(languageHandler.normalizeLanguage(strippedValue, valueTag, svf));
                } else {
                    sink.error(new IOException("Invalid language tag in value: " + valueTag));
                }
            } else {
                sink.success(languageHandler.normalizeLanguage(strippedValue, "en", svf));
            }

        });
    }

    private Mono<Value> normalizeValue(String value, String languageTag) {
        if (value.matches("^<\\w+:(/?/?)[^\\s>]+>$")) {
            value = value.substring(1, value.length() - 1);
            return Mono.just(SimpleValueFactory.getInstance().createIRI(value));
        } else if (value.matches("^\\w+:(/?/?)[^\\s>]+$")) {
            return Mono.just(SimpleValueFactory.getInstance().createLiteral(value));
        } else

            return this.extractLanguageTag(value, languageTag);
    }
}
