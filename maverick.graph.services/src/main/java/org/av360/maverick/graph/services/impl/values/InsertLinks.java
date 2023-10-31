package org.av360.maverick.graph.services.impl.values;

import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.model.errors.requests.InvalidEntityUpdate;
import reactor.core.publisher.Mono;

import java.util.Objects;

public class InsertLinks {
    private final ValueServicesImpl valueServices;

    public InsertLinks(ValueServicesImpl valueServices) {
        this.valueServices = valueServices;
    }

    public Mono<Transaction> insert(String entityKey, String prefixedKey, String targetKey, Boolean replace, SessionContext ctx) {
        return Mono.zip(
                        valueServices.entityServices.resolveAndVerify(entityKey, ctx),
                        valueServices.entityServices.resolveAndVerify(targetKey, ctx),
                        valueServices.schemaServices.resolvePrefixedName(prefixedKey)

                )
                .switchIfEmpty(Mono.error(new InvalidEntityUpdate(entityKey, "Failed to insert link")))
                .flatMap(triple ->
                        valueServices.insertValues.insert(triple.getT1(), triple.getT3(), triple.getT2(), !Objects.isNull(replace) && replace, ctx)
                );
    }
}
