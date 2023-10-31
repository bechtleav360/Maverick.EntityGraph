package org.av360.maverick.graph.services.impl.values;

import jakarta.annotation.Nullable;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.model.errors.InconsistentModelException;
import org.av360.maverick.graph.model.errors.requests.InvalidEntityUpdate;
import org.av360.maverick.graph.model.events.DetailInsertedEvent;
import org.av360.maverick.graph.store.rdf.fragments.RdfEntity;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Statements;
import org.eclipse.rdf4j.model.util.Values;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Optional;

public class InsertDetails {
    private final ValueServicesImpl ctrl;

    public InsertDetails(ValueServicesImpl valueServices) {
        this.ctrl = valueServices;
    }

    public Mono<Transaction> insert(String entityKey, String prefixedValueKey, String prefixedDetailKey, String value, @Nullable String hash, SessionContext ctx) {
        return Mono.zip(
                        ctrl.schemaServices.resolveLocalName(entityKey).flatMap(entityIdentifier -> ctrl.entityServices.get(entityIdentifier, 0, true, ctx)),
                        ctrl.schemaServices.resolvePrefixedName(prefixedValueKey),
                        ctrl.schemaServices.resolvePrefixedName(prefixedDetailKey)
                ).flatMap(tuple -> {
                    RdfEntity entity = tuple.getT1();
                    IRI valuePredicate = tuple.getT2();
                    IRI detailPredicate = tuple.getT3();

                    if (Objects.isNull(hash)) {
                        return this.insertWithoutHash(entity, valuePredicate, detailPredicate, value, ctx);
                    } else {
                        return this.insertWithHash(entity, valuePredicate, detailPredicate, value, hash, ctx);
                    }
                })

                .switchIfEmpty(Mono.just(new RdfTransaction()))
                .doOnSuccess(trx -> {
                    ctrl.eventPublisher.publishEvent(new DetailInsertedEvent(trx));
                });


    }

    private Mono<Transaction> insertWithHash(RdfEntity entity, IRI valuePredicate, IRI detailPredicate, String value, String hash, SessionContext ctx) {
        return Mono.defer(() -> {
                    Optional<Triple> requestedTriple = ctrl.readValues.findTripleByHash(entity, valuePredicate, hash);
                    if(requestedTriple.isEmpty()) return Mono.error(new InvalidEntityUpdate(entity.getIdentifier(), "No value exists for predicate <%s> and hash '%s'".formatted(valuePredicate, hash)));

                    Statement annotationStatement = Statements.statement(requestedTriple.get(), detailPredicate, Values.literal(value), null);
                    return Mono.just(annotationStatement);

                })
                .flatMap(statement -> ctrl.entityServices.getStore(ctx).addStatement(statement, new RdfTransaction()))
                .flatMap(trx -> ctrl.entityServices.getStore(ctx).commit(trx, ctx.getEnvironment()));
    }


    private Mono<Transaction> insertWithoutHash(RdfEntity entity, IRI valuePredicate, IRI detailPredicate, String value, SessionContext ctx) {

        // find triple statement with id - prefixed value key -
        return Mono.defer(() -> {

                    Optional<Value> distinctValue = null;
                    try {
                        distinctValue = entity.findDistinctValue(entity.getIdentifier(), valuePredicate);

                        if (distinctValue.isEmpty()) {
                            return Mono.error(new InvalidEntityUpdate(entity.getIdentifier(), "No value exists for predicate %s".formatted(valuePredicate)));
                        }
                    } catch (InconsistentModelException e) {
                        return Mono.error(new InvalidEntityUpdate(entity.getIdentifier(), "Multiple values for property %s. Use the hash identifier to select the value to update.".formatted(valuePredicate)));
                    }
                    Triple triple = Values.triple(entity.getIdentifier(), valuePredicate, distinctValue.get());
                    Statement annotationStatement = Statements.statement(triple, detailPredicate, Values.literal(value), null);
                    return Mono.just(annotationStatement);

                })
                .flatMap(statement -> ctrl.entityServices.getStore(ctx).addStatement(statement, new RdfTransaction()))
                .flatMap(trx -> ctrl.entityServices.getStore(ctx).commit(trx, ctx.getEnvironment()));
    }
}
