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

package org.av360.maverick.graph.services.api.relations.capabilities;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.model.events.LinkRemovedEvent;
import org.av360.maverick.graph.services.api.Api;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.eclipse.rdf4j.model.IRI;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Slf4j
public class UpdateRelations {
    private final Api api;

    public UpdateRelations(Api api) {
        this.api = api;
    }


    public Mono<Transaction> insert(IRI entityIdentifier, IRI predicate, IRI targetIdentifier, Boolean replace, SessionContext ctx) {
        return  api.values().insert().insert(entityIdentifier, predicate, targetIdentifier, !Objects.isNull(replace) && replace, ctx);
    }

    @Deprecated
    public Mono<Transaction> remove(String entityKey, String prefixedKey, String targetKey, SessionContext ctx) {
        return Mono.zip(
                api.entities().select().resolveAndVerify(entityKey, ctx),
                api.entities().select().resolveAndVerify(targetKey, ctx),
                api.identifiers().prefixes().resolvePrefixedName(prefixedKey)

        ).flatMap(triple ->
                this.remove(triple.getT1(), triple.getT3(), triple.getT2(), ctx)
        );
    }

    public Mono<Transaction> remove(IRI entityIdentifier, IRI predicate, IRI targetIdentifier, SessionContext ctx) {
        return this.api.entities().getStore().asStatementsAware().listStatements(entityIdentifier, predicate, targetIdentifier, ctx.getEnvironment())
                .map(statements -> new RdfTransaction().removes(statements))
                .flatMap(trx -> api.entities().getStore().asCommitable().commit(trx, ctx.getEnvironment()))
                .doOnSuccess(trx -> {
                    api.publishEvent(new LinkRemovedEvent(trx, ctx.getEnvironment()));
                })
                .doOnError(error -> log.error("Failed to remove link due to reason: {}", error.getMessage()));

    }

}
