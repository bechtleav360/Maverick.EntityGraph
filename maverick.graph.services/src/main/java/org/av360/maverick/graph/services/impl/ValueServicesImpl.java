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

package org.av360.maverick.graph.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.av360.maverick.graph.model.annotations.OnRepositoryType;
import org.av360.maverick.graph.model.annotations.RequiresPrivilege;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.model.errors.requests.InvalidEntityUpdate;
import org.av360.maverick.graph.model.rdf.Triples;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.services.EntityServices;
import org.av360.maverick.graph.services.IdentifierServices;
import org.av360.maverick.graph.services.SchemaServices;
import org.av360.maverick.graph.services.ValueServices;
import org.av360.maverick.graph.services.api.Api;
import org.av360.maverick.graph.store.SchemaStore;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Set;

@Slf4j(topic = "graph.svc.values")
@Service
public class ValueServicesImpl implements ValueServices {


    final ApplicationEventPublisher eventPublisher;

    final SchemaServices schemaServices;

    final EntityServices entityServices;

    final IdentifierServices identifierServices;
    private final Api api;


    public ValueServicesImpl(SchemaStore schemaStore,
                             ApplicationEventPublisher eventPublisher,
                             SchemaServices schemaServices,
                             EntityServices entityServices,
                             IdentifierServices identifierServices,
                             Api api

    ) {
        this.eventPublisher = eventPublisher;
        this.schemaServices = schemaServices;
        this.entityServices = entityServices;
        this.identifierServices = identifierServices;


        this.api = api;
    }


    @Override
    @RequiresPrivilege(Authorities.CONTRIBUTOR_VALUE)
    @OnRepositoryType(RepositoryType.ENTITIES)
    public Mono<Transaction> insertValue(String entityKey, String prefixedPoperty, String value, String languageTag, @Nullable Boolean replace, SessionContext ctx) {
        return Mono.zip(
                        api.entities().select().resolveAndVerify(entityKey, ctx),
                        api.identifiers().prefixes().resolvePrefixedName(prefixedPoperty),
                        api.values().insert().normalizeValue(value, languageTag)
                )
                .switchIfEmpty(Mono.error(new InvalidEntityUpdate(entityKey, "Failed to insert value")))
                .flatMap(triple -> this.insertValue(triple.getT1(), triple.getT2(), triple.getT3(), Objects.isNull(replace) ? Boolean.FALSE : replace, ctx));
    }


    @Override
    @RequiresPrivilege(Authorities.CONTRIBUTOR_VALUE)
    @OnRepositoryType(RepositoryType.ENTITIES)
    public Mono<Transaction> insertValue(IRI entityIdentifier, IRI predicate, Value value, @Nullable Boolean replace, SessionContext ctx) {
        return this.api.values().insert().insert(entityIdentifier, predicate, value, replace, ctx)
                .doOnSubscribe(subscription -> {
                    log.info("Insert value for entity <{}> and property <{}> in scope '{}'.", entityIdentifier, predicate, ctx.getEnvironment().getScope());
                });
    }

    /**
     * Deletes a value with a new transaction.  Fails if no entity exists with the given subject
     */
    @Override
    @RequiresPrivilege(Authorities.CONTRIBUTOR_VALUE)
    @OnRepositoryType(RepositoryType.ENTITIES)
    public Mono<Transaction> removeValue(String entityKey, String prefixedPoperty, @Nullable String languageTag, @Nullable String valueIdentifier, SessionContext ctx) {
        return Mono.zip(
                        api.entities().select().resolveAndVerify(entityKey, ctx),
                        api.identifiers().prefixes().resolvePrefixedName(prefixedPoperty)
                )
                .switchIfEmpty(Mono.error(new InvalidEntityUpdate(entityKey, "Failed to remove value")))
                .flatMap(tuple -> this.removeValue(tuple.getT1(), tuple.getT2(), languageTag, valueIdentifier, ctx));
    }

    @Override
    @RequiresPrivilege(Authorities.CONTRIBUTOR_VALUE)
    @OnRepositoryType(RepositoryType.ENTITIES)
    public Mono<Transaction> removeValue(IRI entityIdentifier, IRI predicate, @Nullable String languageTag, @Nullable String valueIdentifier, SessionContext ctx) {
        return this.api.values().remove().remove(entityIdentifier, predicate, languageTag, valueIdentifier, ctx)
                .doOnSubscribe(subscription -> {
                    log.info("Removing value for entity <{}> and property <{}> in scope '{}'.", entityIdentifier, predicate, ctx.getEnvironment().getScope());
                });
    }


    @Override
    @RequiresPrivilege(Authorities.CONTRIBUTOR_VALUE)
    @OnRepositoryType(RepositoryType.ENTITIES)
    public Mono<Transaction> insertLink(String entityKey, String prefixedKey, String targetKey, @Nullable Boolean replace, SessionContext ctx) {
        return Mono.zip(
                        api.entities().select().resolveAndVerify(entityKey, ctx),
                        api.entities().select().resolveAndVerify(targetKey, ctx),
                        api.identifiers().prefixes().resolvePrefixedName(prefixedKey)
                )
                .switchIfEmpty(Mono.error(new InvalidEntityUpdate(entityKey, "Failed to insert link")))
                .flatMap(triple ->
                        this.insertLink(triple.getT1(), triple.getT3(), triple.getT2(), !Objects.isNull(replace) && replace, ctx)
                );
    }

    @Override
    @RequiresPrivilege(Authorities.CONTRIBUTOR_VALUE)
    @OnRepositoryType(RepositoryType.ENTITIES)
    public Mono<Transaction> insertLink(IRI entityIdentifier, IRI predicate, IRI targetIdentifier, @Nullable Boolean replace, SessionContext ctx) {
        return this.api.relations().updates().insert(entityIdentifier, predicate, targetIdentifier, replace, ctx)
                .doOnSubscribe(subscription -> {
                    log.info("Inserting link from entity <{}> to entity <{}> with property <{}> in scope '{}'.", entityIdentifier, targetIdentifier, predicate, ctx.getEnvironment().getScope());
                });
    }


    @Override
    @RequiresPrivilege(Authorities.CONTRIBUTOR_VALUE)
    @OnRepositoryType(RepositoryType.ENTITIES)
    public Mono<Transaction> insertDetail(String entityKey, String prefixedValueKey, String prefixedDetailKey, String value, @Nullable String hash, SessionContext ctx) {
        return Mono.zip(
                        api.identifiers().localIdentifiers().asLocalIRI(entityKey, ctx.getEnvironment()),
                        api.identifiers().prefixes().resolvePrefixedName(prefixedValueKey),
                        api.identifiers().prefixes().resolvePrefixedName(prefixedDetailKey)
                ).flatMap(t -> this.insertDetail(t.getT1(), t.getT2(), t.getT3(), value, hash, ctx));
    }

    @Override
    @RequiresPrivilege(Authorities.CONTRIBUTOR_VALUE)
    @OnRepositoryType(RepositoryType.ENTITIES)
    public Mono<Transaction> insertDetail(IRI entityIdentifier, IRI valuePredicate, IRI detailPredicate, String value, @Nullable String hash, SessionContext ctx) {
        return api.details().inserts().insert(entityIdentifier, valuePredicate, detailPredicate, value, hash, ctx)
                .doOnSubscribe(subscription -> {
                    log.info("Inserting detail <{}> for entity <{}> and property <{}> in scope '{}'.", detailPredicate, entityIdentifier, valuePredicate, ctx.getEnvironment().getScope());
                });
    }


    @Override
    @RequiresPrivilege(Authorities.CONTRIBUTOR_VALUE)
    @OnRepositoryType(RepositoryType.ENTITIES)
    public Mono<Transaction> insertComposite(IRI entityIdentifier, IRI predicate, Resource embeddedNode, Set<Statement> embedded, SessionContext ctx) {
        return this.api.values().insert().insertComposite(entityIdentifier, predicate, embeddedNode, embedded, ctx);
    }


    @Override
    @RequiresPrivilege(Authorities.CONTRIBUTOR_VALUE)
    @OnRepositoryType(RepositoryType.ENTITIES)
    public Mono<Transaction> removeLink(String entityKey, String prefixedProperty, String targetKey, SessionContext ctx) {
        return Mono.zip(
                api.entities().select().resolveAndVerify(entityKey, ctx),
                api.entities().select().resolveAndVerify(targetKey, ctx),
                api.identifiers().prefixes().resolvePrefixedName(prefixedProperty)

        ).flatMap(triple ->
                this.removeLink(triple.getT1(), triple.getT3(), triple.getT2(), ctx)
        );
    }

    @Override
    @RequiresPrivilege(Authorities.CONTRIBUTOR_VALUE)
    @OnRepositoryType(RepositoryType.ENTITIES)
    public Mono<Transaction> removeLink(IRI entityIdentifier, IRI predicate, IRI targetIdentifier, SessionContext ctx) {
        return this.api.relations().updates().remove(entityIdentifier, predicate, targetIdentifier, ctx)
                .doOnSubscribe(subscription -> {
                    log.info("Removing link from entity <{}> to entity <{}> with property <{}> in scope '{}'.", entityIdentifier, targetIdentifier, predicate, ctx.getEnvironment().getScope());
                });
    }

    @Override
    @RequiresPrivilege(Authorities.CONTRIBUTOR_VALUE)
    @OnRepositoryType(RepositoryType.ENTITIES)
    public Mono<Transaction> removeDetail(String entityKey, String prefixedValuePredicate, String prefixedDetailPredicate, String valueHash, SessionContext ctx) {
        return this.api.details().removes().remove(entityKey, prefixedValuePredicate, prefixedDetailPredicate, valueHash, ctx);
    }


    /**
     * Reroutes a statement of an entity, e.g. <entity> <hasProperty> <falseEntity> to <entity> <hasProperty> <rightEntity>.
     * <p>
     * Has to be part of one transaction (one commit call)
     */
    @Override
    @RequiresPrivilege(Authorities.CONTRIBUTOR_VALUE)
    @OnRepositoryType(RepositoryType.ENTITIES)
    public Mono<Transaction> replace(IRI entityIdentifier, IRI predicate, Value oldValue, Value newValue, SessionContext ctx) {
        return this.api.values().insert().replace(entityIdentifier, predicate, oldValue, newValue, ctx);
    }

    @Override
    @RequiresPrivilege(Authorities.READER_VALUE)
    @OnRepositoryType(RepositoryType.ENTITIES)
    public Mono<Triples> listRelations(String entityKey, String prefixedPoperty, SessionContext ctx) {
        return this.api.relations().selects().list(entityKey, prefixedPoperty, ctx);
    }

    @Override
    @RequiresPrivilege(Authorities.READER_VALUE)
    @OnRepositoryType(RepositoryType.ENTITIES)
    public Mono<Triples> listValues(String entityKey, @Nullable String prefixedPoperty, SessionContext ctx) {
        return this.api.values().read().listValues(entityKey, prefixedPoperty, ctx);
    }

    @Override
    @OnRepositoryType(RepositoryType.ENTITIES)
    public Flux<Pair<IRI, Value>> listDetails(String entityKey, String prefixedProperty, String valueIdentifier, SessionContext ctx) {
        return this.api.details().selects().listDetails(entityKey, prefixedProperty, valueIdentifier, ctx);
    }

    @Override
    public Mono<Triples> getValue(String entityKey, String prefixedProperty, @Nullable String languageTag, @Nullable String valueIdentifier, SessionContext ctx) {
        return Mono.zip(
                        api.identifiers().localIdentifiers().asLocalIRI(entityKey, ctx.getEnvironment()),
                        api.identifiers().prefixes().resolvePrefixedName(prefixedProperty)
                )
                .flatMap(pair -> this.api.values().read().getValue(pair.getT1(), pair.getT2(), languageTag, valueIdentifier, ctx));
    }


}
