package org.av360.maverick.graph.services.impl.values;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.model.errors.requests.EntityNotFound;
import org.av360.maverick.graph.model.events.DetailRemovedEvent;
import org.av360.maverick.graph.store.rdf.fragments.RdfEntity;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Values;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j(topic = "graph.svc.detail.del")
public class DeleteDetails {
    private final ValueServicesImpl ctrl;

    public DeleteDetails(ValueServicesImpl ctrl) {
        this.ctrl = ctrl;
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
    Mono<Transaction> removeWithValue(IRI entityIdentifier, IRI predicate, String languageTag, String valueIdentifier, Transaction trx, SessionContext ctx) {
        Set<Flux<Statement>> collect = trx.getRemovedStatements().stream()
                .map(Values::triple)
                .map(triple -> this.ctrl.entityServices.getStore(ctx).listStatements(triple, null, null, ctx.getEnvironment()).flatMapMany(Flux::fromIterable))
                .collect(Collectors.toSet());
        return Flux.merge(collect)
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
     * @param valueHash               A hash value to ensure data integrity for the given detail.
     * @param ctx                     The session context containing session-related information.
     * @return A Mono of the resulting {@link Transaction} after the removal operation.
     * @throws IllegalArgumentException If any of the parameters are invalid.
     * @throws EntityNotFound           If the specified entity or detail is not found.
     */
    public Mono<Transaction> remove(String entityKey, String prefixedValuePredicate, String prefixedDetailPredicate, String languageTag, String valueHash, SessionContext ctx) {
        return Mono.zip(
                        ctrl.schemaServices.resolveLocalName(entityKey).flatMap(entityIdentifier -> ctrl.entityServices.get(entityIdentifier, 0, true, ctx)),
                        ctrl.schemaServices.resolvePrefixedName(prefixedValuePredicate),
                        ctrl.schemaServices.resolvePrefixedName(prefixedDetailPredicate)
                ).flatMap(tuple -> {
                    RdfEntity entity = tuple.getT1();
                    IRI valuePredicate = tuple.getT2();
                    IRI detailPredicate = tuple.getT3();

                    if (Objects.nonNull(valueHash)) {
                        return this.removeWithHash(entity, valuePredicate, detailPredicate, valueHash, ctx);
                    }
                    if (Objects.nonNull(languageTag)) {
                        return this.removeWithLanguageTag(entity, valuePredicate, detailPredicate, languageTag, ctx);
                    }

                    return this.remove(entity, valuePredicate, detailPredicate, ctx);

                })
                .doOnSuccess(trx -> {
                    ctrl.eventPublisher.publishEvent(new DetailRemovedEvent(trx));
                });


    }

    private Mono<Transaction> remove(RdfEntity entity, IRI valuePredicate, IRI detailPredicate, SessionContext ctx) {
        return this.ctrl.insertDetails.buildDetailStatementForSingleValue(entity, valuePredicate, detailPredicate)
                .flatMap(statement -> ctrl.entityServices.getStore(ctx).removeStatement(statement, new RdfTransaction()))
                .flatMap(trx -> ctrl.entityServices.getStore(ctx).commit(trx, ctx.getEnvironment()));
    }

    private Mono<Transaction> removeWithLanguageTag(RdfEntity entity, IRI valuePredicate, IRI detailPredicate, String languageTag, SessionContext ctx) {
        // this.ctrl.readValues.findValueTripleByLanguageTag(entity, valuePredicate, languageTag);
        throw new NotImplementedException("You have to provide the value hash, not a language tag, to delete a detail.");
    }

    private Mono<Transaction> removeWithHash(RdfEntity entity, IRI valuePredicate, IRI detailPredicate, String valueHash, SessionContext ctx) {
        return this.ctrl.insertDetails.buildDetailStatementForValueWithHash(entity, valuePredicate, detailPredicate, valueHash)
                .flatMap(statement -> ctrl.entityServices.getStore(ctx).removeStatement(statement, new RdfTransaction()))
                .flatMap(trx -> ctrl.entityServices.getStore(ctx).commit(trx, ctx.getEnvironment()));
    }



}
