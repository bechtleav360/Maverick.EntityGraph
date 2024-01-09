/*
 * Copyright (c) 2024.
 *
 *  Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 *  European Commission - subsequent versions of the EUPL (the "Licence");
 *
 *  You may not use this work except in compliance with the Licence.
 *  You may obtain a copy of the Licence at:
 *
 *  https://joinup.ec.europa.eu/software/page/eupl5
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the Licence for the specific language governing permissions and limitations under the Licence.
 *
 */

package org.av360.maverick.graph.services.api.values.capabilities;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.model.errors.requests.EntityNotFound;
import org.av360.maverick.graph.model.errors.requests.InvalidEntityUpdate;
import org.av360.maverick.graph.model.events.ValueInsertedEvent;
import org.av360.maverick.graph.model.events.ValueReplacedEvent;
import org.av360.maverick.graph.services.api.Api;
import org.av360.maverick.graph.store.rdf.fragments.RdfFragment;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Set;

import static org.av360.maverick.graph.services.api.values.ValuesUtils.extractLanguageTag;

@Slf4j
public class InsertValues {


    private final Api api;

    public InsertValues(Api services) {
        this.api = services;
    }

    public Mono<Transaction> insert(String entityKey, String prefixedPoperty, String value, String languageTag, Boolean replace, SessionContext ctx) {
        return Mono.zip(
                        api.entities().select().resolveAndVerify(entityKey, ctx),
                        api.identifiers().prefixes().resolvePrefixedName(prefixedPoperty),
                        this.normalizeValue(value, languageTag)
                )
                .switchIfEmpty(Mono.error(new InvalidEntityUpdate(entityKey, "Failed to insert value")))
                .flatMap(triple -> this.insert(triple.getT1(), triple.getT2(), triple.getT3(), Objects.isNull(replace) ? Boolean.FALSE : replace, ctx));
    }


    public Mono<Transaction> insert(IRI entityIdentifier, IRI predicate, Value value, @Nullable Boolean replace, SessionContext ctx) {
        return this.insertStatement(entityIdentifier, predicate, value, new RdfTransaction(), !Objects.isNull(replace) && replace, ctx)
                .doOnSuccess(trx -> {
                    api.publishEvent(new ValueInsertedEvent(trx));
                });
    }


    private Mono<Value> normalizeValue(String value, String languageTag) {
        if (value.matches("^<\\w+:(/?/?)[^\\s>]+>$")) {
            value = value.substring(1, value.length() - 1);
            return Mono.just(SimpleValueFactory.getInstance().createIRI(value));
        } else if (value.matches("^\\w+:(/?/?)[^\\s>]+$")) {
            return Mono.just(SimpleValueFactory.getInstance().createLiteral(value));
        } else

            return extractLanguageTag(value, languageTag);
    }




    public Mono<Transaction> insertComposite(IRI entityIdentifier, IRI predicate, Resource embeddedNode, Set<Statement> embedded, SessionContext ctx) {
        if(embeddedNode.isBNode()) return Mono.error(new InvalidEntityUpdate(entityIdentifier, "Trying to link to shared node as anonymous node."));

        Transaction transaction = new RdfTransaction();
        return this.api.entities().select().get(entityIdentifier, ctx)
                .switchIfEmpty(Mono.error(new EntityNotFound(entityIdentifier.stringValue())))
                .map(entity -> transaction.affects(entity.getModel()))
                .map(trx -> {
                    trx.inserts(embedded);
                    trx.updates(entityIdentifier, predicate, embeddedNode);
                    return trx;
                })
                .flatMap(trx -> this.api.entities().getStore().asCommitable().commit(trx, ctx.getEnvironment()))
                .switchIfEmpty(Mono.just(transaction));
    }


    public Mono<Transaction> replace(IRI entityIdentifier, IRI predicate, Value oldValue, Value newValue, SessionContext ctx) {
        Transaction transaction = new RdfTransaction()
                .removes(entityIdentifier, predicate, oldValue)
                .inserts(entityIdentifier, predicate, newValue);

        return this.api.entities().getStore().asCommitable().commit(transaction, ctx.getEnvironment())
                .doOnSuccess(trx -> {
                    this.api.publishEvent(new ValueReplacedEvent(trx));
                });

    }


    Mono<Transaction> insertStatement(IRI entityIdentifier, IRI predicate, Value value, Transaction transaction, boolean replace, SessionContext ctx) {

        Triple triple = SimpleValueFactory.getInstance().createTriple(entityIdentifier, predicate, value);

        return this.api.entities().select().get(entityIdentifier, ctx)
                .switchIfEmpty(Mono.error(new EntityNotFound(entityIdentifier.stringValue())))
                .doOnNext(entity -> transaction.affects(entity.getModel()))
                .flatMap(entity -> {

                    // linking to bnodes is forbidden
                    if (triple.getObject().isBNode()) {
                        log.trace("Insert link for {} to anonymous node is forbidden.", entityIdentifier);
                        return Mono.error(new InvalidEntityUpdate(entityIdentifier, "Trying to link to anonymous node."));
                    } else if (triple.getObject().isIRI()) {
                        return this.buildTransactionForIRIStatement(triple, entity, transaction, replace, ctx);
                    } else if (triple.getObject().isLiteral()) {
                        return this.buildTransactionForLiteralStatement(triple, entity, transaction, replace, ctx);
                    } else return Mono.empty();

                })
                .map(trx -> trx.inserts(entityIdentifier, predicate, value))
                .flatMap(trx -> this.api.entities().getStore().asCommitable().commit(trx, ctx.getEnvironment()))
                .switchIfEmpty(Mono.just(transaction));

    }

    private Mono<Transaction> buildTransactionForIRIStatement(Triple statement, RdfFragment entity, Transaction transaction, boolean replace, SessionContext ctx) {
        // check if entity already has this statement. If yes, we do nothing
        if (statement.getObject().isIRI() && entity.hasStatement(statement) && !replace) {
            log.trace("Entity {} already has a link '{}' for predicate '{}', ignoring update.", entity.getIdentifier(), statement.getObject(), statement.getPredicate());
            return Mono.empty();
        } else {
            return Mono.just(transaction.inserts(statement));
        }
    }

    private Mono<Transaction> buildTransactionForLiteralStatement(Triple triple, RdfFragment entity, Transaction transaction, boolean replace, SessionContext ctx) {
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
                            transaction.removes(statement);
                        }
                    } else {
                        // entity already has a value for this predicate. It has no language tag. If an existing value has a language tag, we throw an error. If not, we remove it.
                        if (currentValue.getLanguage().isPresent())
                            throw new InvalidEntityUpdate(entity.getIdentifier(), "This value already exists with a language tag within this entity. Please add the tag.");

                        transaction.removes(statement);
                    }

                }
                return Mono.just(transaction);

            } catch (InvalidEntityUpdate e) {
                return Mono.error(e);
            }

        } else return Mono.just(transaction);

    }
}
