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

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.errors.requests.EntityNotFound;
import org.av360.maverick.graph.services.api.Api;
import org.av360.maverick.graph.store.IndividualsStore;
import org.av360.maverick.graph.store.rdf.fragments.RdfFragment;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import reactor.core.publisher.Mono;

@Slf4j
public class ReadEntities {
    private final Api api;
    private final IndividualsStore entityStore;

    public ReadEntities(Api parent, IndividualsStore entityStore) {
        this.api = parent;
        this.entityStore = entityStore;
    }

    /**
     * Retrieves a complete entity representation (identifier, values and relations) from store.
     *
     * @param authentication The current authentication
     * @param entityIri      The unique entity URI
     * @param details
     * @param depth          how many levels of neigbours to include (0 is entity only, 1 is direct neighbours)
     * @return Entity as Mono
     */
    public Mono<RdfFragment> get(Resource entityIri, boolean details, int depth, SessionContext ctx) {
        return entityStore.asFragmentable().getFragment(entityIri, depth, details, ctx.getEnvironment())
                .switchIfEmpty(Mono.error(new EntityNotFound(entityIri)));
    }

    /**
     * Retrieves an entity representation (identifier, values and relations) with its direct neighbours from store.
     *
     * @param entityIri The unique entity identifier
     * @param authentication   The current authentication
     * @return Entity as Mono
     */
    public Mono<RdfFragment> get(Resource entityIri, SessionContext ctx) {
        return this.get(entityIri, false, 1, ctx);
    }



    public Mono<IRI> resolveAndVerify(String key, SessionContext ctx) {
        return this.api.identifiers().localIdentifiers().asLocalIRI(key, ctx.getEnvironment())
                .filterWhen(iri -> this.exists(iri, ctx))
                .switchIfEmpty(Mono.error(new EntityNotFound(key)))
                .doOnSuccess(res -> log.trace("Resolved entity key {} to qualified identifier {}", key, res.stringValue()));
    }

    public Mono<Boolean> exists(IRI entityIri, SessionContext ctx) {
        return entityStore.asFragmentable().exists(entityIri, ctx.getEnvironment());
    }
}
