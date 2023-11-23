package org.av360.maverick.graph.services.impl.values;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.model.errors.requests.EntityNotFound;
import org.av360.maverick.graph.model.errors.requests.InvalidEntityUpdate;
import org.av360.maverick.graph.model.errors.store.InvalidEntityModelException;
import org.av360.maverick.graph.model.events.DetailRemovedEvent;
import org.av360.maverick.graph.store.rdf.fragments.Fragment;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.eclipse.rdf4j.model.IRI;
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
    Mono<Transaction> removeDetailStatements(Transaction trx, SessionContext ctx) {
        Set<Flux<Statement>> collect = trx.getRemovedStatements().stream()
                .map(statement ->
                        this.ctrl.entityServices.getStore(ctx).asStatementsAware().listStatements(null, RDF.SUBJECT, statement.getSubject(), ctx.getEnvironment())
                                .flatMapMany(Flux::fromIterable)
                                .map(Statement::getSubject)
                                .flatMap(subject -> this.ctrl.entityServices.getStore(ctx).asStatementsAware().listStatements(subject, null, null, ctx.getEnvironment()))
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
                        ctrl.identifierServices.asIRI(entityKey, ctx.getEnvironment()).flatMap(entityIdentifier -> ctrl.entityServices.get(entityIdentifier, 0, true, ctx)),
                        ctrl.schemaServices.resolvePrefixedName(prefixedValuePredicate),
                        ctrl.schemaServices.resolvePrefixedName(prefixedDetailPredicate)
                ).flatMap(tuple -> {
                    Fragment entity = tuple.getT1();
                    IRI valuePredicate = tuple.getT2();
                    IRI detailPredicate = tuple.getT3();

                    if (Objects.nonNull(valueIdentifier)) {
                        return this.removeWithHash(entity, valuePredicate, detailPredicate, valueIdentifier, ctx);
                    } else return this.remove(entity, valuePredicate, detailPredicate, ctx);

                })
                .doOnSuccess(trx -> {
                    ctrl.eventPublisher.publishEvent(new DetailRemovedEvent(trx));
                });


    }

    private Mono<Transaction> remove(Fragment entity, IRI valuePredicate, IRI detailPredicate, SessionContext ctx) {
        try {
            Optional<Triple> requestedTriple = this.ctrl.readValues.findSingleValueTriple(entity, valuePredicate);
            if (requestedTriple.isEmpty())
                return Mono.error(new InvalidEntityUpdate(entity.getIdentifier(), "No value exists in entity <%s> for predicate <%s>".formatted(entity.getIdentifier(), valuePredicate)));


            Transaction trx = new RdfTransaction().forRemoval(entity.listStatements(requestedTriple.get(), detailPredicate, null));
            return ctrl.entityServices.getStore(ctx).asCommitable().commit(trx, ctx.getEnvironment());

        } catch (InvalidEntityModelException e) {
            return Mono.error(e);
        }

    }


    private Mono<Transaction> removeWithHash(Fragment entity, IRI valuePredicate, IRI detailPredicate, String valueHash, SessionContext ctx) {
        Optional<Triple> requestedTriple = this.ctrl.readValues.findValueTripleByHash(entity, valuePredicate, valueHash);
        if (requestedTriple.isEmpty())
            return Mono.error(new InvalidEntityUpdate(entity.getIdentifier(), "No value exists in entity <%s> for predicate <%s> and hash '%s'".formatted(entity.getIdentifier(), valuePredicate, valueHash)));

        Transaction trx = new RdfTransaction().forRemoval(entity.listStatements(requestedTriple.get(), detailPredicate, null));
        return ctrl.entityServices.getStore(ctx).asCommitable().commit(trx, ctx.getEnvironment());
    }


}
