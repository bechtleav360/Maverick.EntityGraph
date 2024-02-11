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

package org.av360.maverick.graph.services.api.entities.capabilities;

import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.model.events.EntityDeletedEvent;
import org.av360.maverick.graph.services.api.Api;
import org.av360.maverick.graph.store.IndividualsStore;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.eclipse.rdf4j.model.IRI;
import reactor.core.publisher.Mono;

public class UpdateEntities {
    private final Api api;
    private final IndividualsStore entityStore;

    public UpdateEntities(Api parent, IndividualsStore entityStore) {
        this.api = parent;
        this.entityStore = entityStore;
    }


    public Mono<Transaction> commit(Transaction trx, Environment environment) {
        return this.entityStore.asCommitable().commit(trx, environment);
    }

    public Mono<Transaction> delete(IRI entityIri, SessionContext ctx) {
        return this.entityStore.asStatementsAware().listStatements(entityIri, null, null, ctx.getEnvironment())
                .map(statements -> new RdfTransaction().removes(statements))
                .flatMap(trx -> this.entityStore.asCommitable().commit(trx, ctx.getEnvironment()))
                .doOnSuccess(transaction -> {
                    api.publishEvent(new EntityDeletedEvent(transaction, ctx.getEnvironment()));
                });
    }
}
