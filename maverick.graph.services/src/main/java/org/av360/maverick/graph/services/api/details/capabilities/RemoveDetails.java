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

package org.av360.maverick.graph.services.api.details.capabilities;

import com.apicatalog.jsonld.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.model.errors.requests.EntityNotFound;
import org.av360.maverick.graph.model.errors.requests.InvalidEntityUpdate;
import org.av360.maverick.graph.model.errors.store.InvalidEntityModelException;
import org.av360.maverick.graph.model.events.DetailRemovedEvent;
import org.av360.maverick.graph.services.api.Api;
import org.av360.maverick.graph.services.api.values.ValuesUtils;
import org.av360.maverick.graph.store.rdf.fragments.RdfFragment;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j(topic = "graph.svc.detail.del")
public class RemoveDetails {
    private final Api api;

    public RemoveDetails(Api ctrl) {
        this.api = ctrl;
    }

    /**
     * Will remove all detail statements for a given value, i.e. all statements where the value statement is a subject triple.
     *
     * @param entityIdentifier
     * @param predicate
     * @param languageTag
     * @param valueIdentifier
     * @param trx
     * @param ctx
     * @return
     */
    Mono<Transaction> removeDetailStatements(Transaction trx, SessionContext ctx) {
        Set<Flux<Statement>> collect = trx.getRemovedStatements().stream()
                .map(statement ->
                        this.api.entities().getStore().asStatementsAware().listStatements(null, RDF.SUBJECT, statement.getSubject(), ctx.getEnvironment())
                                .flatMapMany(Flux::fromIterable)
                                .map(Statement::getSubject)
                                .flatMap(subject -> this.api.entities().getStore().asStatementsAware().listStatements(subject, null, null, ctx.getEnvironment()))
                                .flatMap(Flux::fromIterable))
                .collect(Collectors.toSet());


        // following for native rdf star support in lmdb
        /* Set<Flux<Statement>> collect = trx.getRemovedStatements().stream()
                .map(Values::triple)
                .map(triple -> this.ctrl.entityServices.getStore(ctx).asStatementsAware().listStatements(triple, null, null, ctx.getEnvironment()).flatMapMany(Flux::fromIterable))
                .collect(Collectors.toSet());*/
        return Flux.merge(collect)
                .collectList()
                .map(trx::removes);


    }


    public Mono<Transaction> removeAllDetails(IRI entityIdentifier, IRI predicate, String languageTag, String valueIdentifier, Transaction trx, SessionContext ctx) {
        if(StringUtils.isNotBlank(valueIdentifier)) {
            return removeAllDetailsUsingValueIdentifier(entityIdentifier, predicate, valueIdentifier, trx, ctx);
        } else if (StringUtils.isNotBlank(languageTag)) {
            return removeAllDetailsUsingLanguageTag(entityIdentifier, predicate, valueIdentifier, trx, ctx);
        } else {
            return removeAllDetailsWithoutQualifier(entityIdentifier, predicate, trx, ctx);
        }
    }

    private Mono<Transaction> removeAllDetailsUsingLanguageTag(IRI entityIdentifier, IRI predicate, String languageTag, Transaction trx, SessionContext ctx) {
        return this.api.entities().getStore().asStatementsAware().listStatements(null, RDF.SUBJECT, entityIdentifier, ctx.getEnvironment()) // returns all details for all values of the current entity
                .flatMapMany(Flux::fromIterable)
                .filterWhen(detailsStatement -> this.api.entities().getStore().asStatementsAware().hasStatement(detailsStatement.getSubject(), RDF.PREDICATE, predicate, ctx.getEnvironment()))
                .flatMap(detailsStatement ->
                        this.api.entities().getStore().asStatementsAware().listStatements(detailsStatement.getSubject(), RDF.OBJECT, null, ctx.getEnvironment()) // returns only details affecting the value property (can still be multiple)
                                .map(statements -> statements.stream()
                                        .filter(statement -> statement.getObject().isLiteral()
                                                && ((Literal) statement.getObject()).getLanguage().isPresent()
                                                && ((Literal) statement.getObject()).getLanguage().get().equalsIgnoreCase(languageTag)
                                        )
                                        .collect(Collectors.toSet())
                                        .stream()
                                        .findFirst())  // compute value identifier and compare with parameter value
                                .map(optionalStatement -> optionalStatement.map(Statement::getSubject))
                                .flatMap(optionalSubject -> optionalSubject.map(resource -> this.api.entities().getStore().asStatementsAware().listStatements(resource, null, null, ctx.getEnvironment())).orElseGet(() -> Mono.just(Set.of()))))
                .flatMapIterable(set -> set)
                .collectList()
                .map(trx::removes);
    }

    public Mono<Transaction> removeAllDetailsWithoutQualifier(IRI entityIdentifier, IRI predicate,  Transaction trx, SessionContext ctx) {
        return this.api.entities().getStore().asStatementsAware().listStatements(null, RDF.SUBJECT, entityIdentifier, ctx.getEnvironment()) // returns all details for all values of the current entity
                .flatMapMany(Flux::fromIterable)
                .filterWhen(detailsStatement -> this.api.entities().getStore().asStatementsAware().hasStatement(detailsStatement.getSubject(), RDF.PREDICATE, predicate, ctx.getEnvironment()))
                .flatMap(detailsStatement -> this.api.entities().getStore().asStatementsAware().listStatements(detailsStatement.getSubject(), null, null, ctx.getEnvironment()))
                .flatMapIterable(set -> set)
                .collectList()
                .map(trx::removes);
    }


    private Mono<Transaction> removeAllDetailsUsingValueIdentifier(IRI entityIdentifier, IRI predicate, String valueIdentifier, Transaction trx, SessionContext ctx) {
        return this.api.entities().getStore().asStatementsAware().listStatements(null, RDF.SUBJECT, entityIdentifier, ctx.getEnvironment()) // returns all details for all values of the current entity
                .flatMapMany(Flux::fromIterable)
                .flatMap(detailsStatement ->
                        this.api.entities().getStore().asStatementsAware().listStatements(detailsStatement.getSubject(), RDF.OBJECT, null, ctx.getEnvironment()) // returns only details affecting the value property (can still be multiple)
                                .map(statements -> statements.stream()
                                        .filter(statement -> ValuesUtils.generateHashForValue(predicate, statement.getObject()).equalsIgnoreCase(valueIdentifier))
                                        .collect(Collectors.toSet())
                                        .stream()
                                        .findFirst())  // compute value identifier and compare with parameter value
                                .map(optionalStatement -> optionalStatement.map(Statement::getSubject))
                                .flatMap(optionalSubject -> optionalSubject.map(resource -> this.api.entities().getStore().asStatementsAware().listStatements(resource, null, null, ctx.getEnvironment())).orElseGet(() -> Mono.just(Set.of()))))
                .flatMapIterable(set -> set)
                .collectList()
                .map(trx::removes);
    }

    /**
     * Removes the detail for the given value, identified by the given prefixed predicate.
     *
     * @param entityKey               The key of the entity from which the detail is to be removed.
     * @param prefixedValuePredicate  The prefixed value predicate associated with the entity.
     * @param prefixedDetailPredicate The prefixed detail predicate associated with the value.
     * @param languageTag             The IETF BCP 47 language tag indicating the language of the detail.
     * @param valueIdentifier         A hash value to ensure data integrity for the given detail.
     * @param ctx                     The session context containing session-related information.
     * @return A Mono of the resulting {@link Transaction} after the removal operation.
     * @throws IllegalArgumentException If any of the parameters are invalid.
     * @throws EntityNotFound           If the specified entity or detail is not found.
     */
    public Mono<Transaction> remove(String entityKey, String prefixedValuePredicate, String prefixedDetailPredicate, String valueIdentifier, SessionContext ctx) {
        return Mono.zip(
                        this.api.identifiers().localIdentifiers().asLocalIRI(entityKey, ctx.getEnvironment())
                                .flatMap(entityIdentifier -> api.entities().select().get(entityIdentifier, true, 0, ctx)),
                        api.identifiers().prefixes().resolvePrefixedName(prefixedValuePredicate),
                        api.identifiers().prefixes().resolvePrefixedName(prefixedDetailPredicate)
                ).flatMap(tuple -> {
                    RdfFragment entity = tuple.getT1();
                    IRI valuePredicate = tuple.getT2();
                    IRI detailPredicate = tuple.getT3();

                    if (Objects.nonNull(valueIdentifier)) {
                        return this.removeWithHash(entity, valuePredicate, detailPredicate, valueIdentifier, ctx);
                    } else return this.remove(entity, valuePredicate, detailPredicate, ctx);

                })
                .doOnSuccess(trx -> {
                    api.publishEvent(new DetailRemovedEvent(trx));
                });


    }

    private Mono<Transaction> remove(RdfFragment entity, IRI valuePredicate, IRI detailPredicate, SessionContext ctx) {
        try {
            Optional<Triple> requestedTriple = this.api.values().read().findSingleValueTriple(entity, valuePredicate);
            if (requestedTriple.isEmpty())
                return Mono.error(new InvalidEntityUpdate(entity.getIdentifier(), "No value exists in entity <%s> for predicate <%s>".formatted(entity.getIdentifier(), valuePredicate)));


            Transaction trx = new RdfTransaction().removes(entity.listStatements(requestedTriple.get(), detailPredicate, null));
            return api.entities().getStore().asCommitable().commit(trx, ctx.getEnvironment());

        } catch (InvalidEntityModelException e) {
            return Mono.error(e);
        }

    }


    private Mono<Transaction> removeWithHash(RdfFragment entity, IRI valuePredicate, IRI detailPredicate, String valueHash, SessionContext ctx) {
        Optional<Triple> requestedTriple = api.values().read().findValueTripleByHash(entity, valuePredicate, valueHash);
        if (requestedTriple.isEmpty())
            return Mono.error(new InvalidEntityUpdate(entity.getIdentifier(), "No value exists in entity <%s> for predicate <%s> and hash '%s'".formatted(entity.getIdentifier(), valuePredicate, valueHash)));

        Transaction trx = new RdfTransaction().removes(entity.listStatements(requestedTriple.get(), detailPredicate, null));
        return api.entities().getStore().asCommitable().commit(trx, ctx.getEnvironment());
    }


}
