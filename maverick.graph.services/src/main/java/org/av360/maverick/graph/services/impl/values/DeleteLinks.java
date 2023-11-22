package org.av360.maverick.graph.services.impl.values;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.model.events.LinkRemovedEvent;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.eclipse.rdf4j.model.IRI;
import reactor.core.publisher.Mono;
@Slf4j(topic = "graph.svc.links.del")
public class DeleteLinks {
    private final ValueServicesImpl ctrl;

    public DeleteLinks(ValueServicesImpl valueServices) {
        this.ctrl = valueServices;
    }

    public Mono<Transaction> remove(String entityKey, String prefixedProperty, String targetKey, SessionContext ctx) {
        return Mono.zip(
                ctrl.entityServices.resolveAndVerify(entityKey, ctx),
                ctrl.entityServices.resolveAndVerify(targetKey, ctx),
                ctrl.schemaServices.resolvePrefixedName(prefixedProperty)

        ).flatMap(triple ->
                this.removeLinkStatement(triple.getT1(), triple.getT3(), triple.getT2(), new RdfTransaction(), ctx)
        ).doOnSuccess(trx -> {
            ctrl.eventPublisher.publishEvent(new LinkRemovedEvent(trx));
        }).doOnError(error -> log.error("Failed to remove link due to reason: {}", error.getMessage()));
    }

    private Mono<Transaction> removeLinkStatement(IRI entityIdentifier, IRI predicate, IRI targetIdentifier, Transaction transaction, SessionContext ctx) {
        return ctrl.entityServices.getStore(ctx).asStatementsAware().listStatements(entityIdentifier, predicate, targetIdentifier, ctx.getEnvironment())
                .map(transaction::forRemoval)
                .flatMap(trx -> ctrl.entityServices.getStore(ctx).asCommitable().commit(trx, ctx.getEnvironment()));

    }
}
