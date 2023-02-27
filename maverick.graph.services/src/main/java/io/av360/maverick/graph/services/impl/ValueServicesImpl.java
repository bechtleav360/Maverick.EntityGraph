package io.av360.maverick.graph.services.impl;

import io.av360.maverick.graph.model.errors.EntityNotFound;
import io.av360.maverick.graph.model.errors.InvalidEntityUpdate;
import io.av360.maverick.graph.model.rdf.LocalIRI;
import io.av360.maverick.graph.services.ValueServices;
import io.av360.maverick.graph.services.events.ValueInsertedEvent;
import io.av360.maverick.graph.services.events.ValueRemovedEvent;
import io.av360.maverick.graph.services.events.ValueReplacedEvent;
import io.av360.maverick.graph.store.EntityStore;
import io.av360.maverick.graph.store.SchemaStore;
import io.av360.maverick.graph.store.rdf.models.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.LanguageHandler;
import org.eclipse.rdf4j.rio.LanguageHandlerRegistry;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j(topic = "graph.service.values")
@Service
public class ValueServicesImpl implements ValueServices {


    private final EntityStore entityStore;
    private final SchemaStore schemaStore;

    private final ApplicationEventPublisher eventPublisher;

    public ValueServicesImpl(EntityStore entityStore,
                             SchemaStore schemaStore,
                             ApplicationEventPublisher eventPublisher) {
        this.entityStore = entityStore;
        this.schemaStore = schemaStore;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Extracts the language tag.
     * Rules:
     * - tag in request parameter > tag in value
     * - default lang tag is "en"
     * - if one is invalid, we take the other (and write a warning into the logs)
     *
     * @param id
     * @param value    the literal value as string
     * @param paramTag the request parameter for the language tag, can be null
     * @return the identified language tag
     */
    private Mono<String> extractLanguageTag(String id, String value, @Nullable String paramTag) {
        return Mono.create(sink -> {
            LanguageHandler languageHandler = LanguageHandlerRegistry.getInstance().get(LanguageHandler.BCP47).orElseThrow();

            String valueTag = value.matches(".*@\\w\\w-?[\\w\\d-]*$") ? value.substring(value.lastIndexOf('@') + 1) : "";

            if(StringUtils.isNotBlank(paramTag)) {
                if(languageHandler.isRecognizedLanguage(paramTag)  && languageHandler.verifyLanguage(value, paramTag)) {
                    sink.success(paramTag);
                } else {
                    sink.error(new IOException("Invalid language tag in parameter: "+paramTag));
                }
            }

            else if (StringUtils.isNotBlank(valueTag)) {
                if(languageHandler.isRecognizedLanguage(valueTag) && languageHandler.verifyLanguage(value, valueTag)) {
                    sink.success(valueTag);
                } else {
                    sink.error(new IOException("Invalid language tag in value: "+valueTag));
                }
            } else {
                sink.success("en");
            }

        });




    }

    @Override
    public Mono<Transaction> insertValue(String id, String propertyPrefix, String property, String value, String languageTag, Authentication authentication) {
        return this.extractLanguageTag(id, value, languageTag)
                .map(tag -> {
                    LanguageHandler languageHandler = LanguageHandlerRegistry.getInstance().get(LanguageHandler.BCP47).orElseThrow();
                    return languageHandler.normalizeLanguage(value, tag, SimpleValueFactory.getInstance());
                })
                .flatMap(literal -> this.insertValue(LocalIRI.withDefaultNamespace(id),
                        LocalIRI.withDefinedNamespace(schemaStore.getNamespaceFor(propertyPrefix), property),
                        literal,
                        authentication));
    }

    @Override
    public Mono<Transaction> insertValue(Resource entityIdentifier, IRI predicate, Value value, Authentication authentication) {
        return this.insertValue(entityIdentifier, predicate, value, new Transaction(), authentication)
                .doOnSuccess(trx -> {
                    eventPublisher.publishEvent(new ValueInsertedEvent(trx));
                });

    }

    Mono<Transaction> insertValue(Resource entityIdentifier, IRI predicate, Value value, Transaction transaction, Authentication authentication) {

        return this.entityStore.getEntity(entityIdentifier, authentication)
                .switchIfEmpty(Mono.error(new EntityNotFound(entityIdentifier.stringValue())))
                .map(entity -> Pair.of(entity, transaction.affected(entity)))
                .flatMap(pair -> {
                    // linking to bnodes is forbidden
                    if (value.isBNode()) {
                        log.trace("Insert link for {} to anonymous node is forbidden.", entityIdentifier);
                        return Mono.error(new InvalidEntityUpdate(entityIdentifier, "Trying to link to anonymous node."));
                    } else return Mono.just(pair);
                })
                .flatMap(pair -> {
                    // check if entity already has this statement. If yes, we do nothing
                    if (value.isIRI() && pair.getLeft().hasStatement(entityIdentifier, predicate, value)) {
                        log.trace("Entity {} already has a link '{}' for predicate '{}', ignoring update.", entityIdentifier, value, predicate);
                        return Mono.empty();
                    } else return Mono.just(pair);
                })
                .flatMap(pair -> {
                    // check if entity already has this literal with a different value. If yes, we remove it first (but only if it also has the same language tag)
                    if (value.isLiteral() && pair.getLeft().hasStatement(entityIdentifier, predicate, null)) {
                        log.trace("Entity {} already has a value for predicate '{}'.", entityIdentifier, predicate);
                        Literal updateValue = (Literal) value;

                        try {
                            for (Statement statement : pair.getLeft().listStatements(entityIdentifier, predicate, null)) {
                                if (!statement.getObject().isLiteral())
                                    throw new InvalidEntityUpdate(entityIdentifier, "Replacing an existing link to another entity with a value is not allowed. ");

                                Literal currentValue = (Literal) statement.getObject();
                                if (updateValue.getLanguage().isPresent() && currentValue.getLanguage().isPresent()) {
                                    // entity already has a value for this predicate. It has a language tag. If another value with the same language tag exists, we remove it.
                                    if (StringUtils.equals(currentValue.getLanguage().get(), updateValue.getLanguage().get())) {
                                        this.entityStore.removeStatement(statement, pair.getRight());
                                    }
                                } else {
                                    // entity already has a value for this predicate. It has no language tag. If an existing value has a language tag, we throw an error. If not, we remove it.
                                    if (currentValue.getLanguage().isPresent())
                                        throw new InvalidEntityUpdate(entityIdentifier, "This value already exists with a language tag within this entity. Please add the tag.");

                                    this.entityStore.removeStatement(statement, pair.getRight());
                                }

                            }
                            return Mono.just(pair);

                        } catch (InvalidEntityUpdate e) {
                            return Mono.error(e);
                        }

                    } else return Mono.just(pair);

                })
                .flatMap(pair -> this.entityStore.addStatement(entityIdentifier, predicate, value, transaction))
                .flatMap(trx -> this.entityStore.commit(trx, authentication))
                .switchIfEmpty(Mono.just(transaction));

    }

    /**
     * Deletes a value with a new transaction.  Fails if no entity exists with the given subject
     */
    @Override
    public Mono<Transaction> removeValue(String id, String predicatePrefix, String predicateKey, String lang, Authentication authentication) {
        return this.removeValue(LocalIRI.withDefaultNamespace(id),
                LocalIRI.withDefinedNamespace(schemaStore.getNamespaceFor(predicatePrefix), predicateKey),
                lang,
                authentication);
    }


    /**
     * Deletes a value with a new transaction. Fails if no entity exists with the given subject
     */
    @Override
    public Mono<Transaction> removeValue(Resource entityIdentifier, IRI predicate, String lang, Authentication authentication) {
        return this.removeValue(entityIdentifier, predicate, lang, new Transaction(), authentication)
                .doOnSuccess(trx -> {
                    eventPublisher.publishEvent(new ValueRemovedEvent(trx));
                });
    }

    /**
     * Internal method to remove a value within an existing transaction.
     */
    Mono<Transaction> removeValue(Resource entityIdentifier, IRI predicate, String lang, Transaction transaction, Authentication authentication) {

        return this.entityStore.listStatements(entityIdentifier, predicate, null, authentication)
                .flatMap(statements -> {
                    if (statements.size() > 1) {
                        List<Statement> statementsToRemove = new ArrayList<>();
                        if (StringUtils.isEmpty(lang)) {
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
                                if (StringUtils.equals(currentLiteral.getLanguage().orElse("invalid"), lang)) {
                                    statementsToRemove.add(st);
                                }
                            }
                        }


                        return this.entityStore.removeStatements(statementsToRemove, transaction);
                    } else {
                        if (statements.size() == 1 && statements.get(0).getObject().isIRI()) {
                            return Mono.error(new InvalidEntityUpdate(entityIdentifier, "Invalid to remove links via the values api."));
                        }
                        return this.entityStore.removeStatements(statements, transaction);
                    }

                })
                .flatMap(trx -> this.entityStore.commit(trx, authentication));
    }

    /**
     * Reroutes a statement of an entity, e.g. <entity> <hasProperty> <falseEntity> to <entity> <hasProperty> <rightEntity>.
     * <p>
     * Has to be part of one transaction (one commit call)
     */
    public Mono<Transaction> replaceValue(Resource entityIdentifier, IRI predicate, Value oldObject, Value newObject, Authentication authentication) {
        return this.entityStore.removeStatement(entityIdentifier, predicate, oldObject, new Transaction())
                .flatMap(trx -> this.entityStore.addStatement(entityIdentifier, predicate, newObject, trx))
                .flatMap(trx -> this.entityStore.commit(trx, authentication))
                .doOnSuccess(trx -> {
                    eventPublisher.publishEvent(new ValueReplacedEvent(trx));
                });
    }


}
