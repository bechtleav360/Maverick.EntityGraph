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

import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.rdf.Triples;
import org.av360.maverick.graph.services.api.Api;
import org.av360.maverick.graph.store.rdf.fragments.RdfFragment;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import reactor.core.publisher.Mono;

public class ReadRelations {
    private final Api api;

    public ReadRelations(Api api) {
        this.api = api;
    }

    public Mono<Triples> list(String entityKey, String prefixedPoperty, SessionContext ctx) {
        return Mono.zip(
                api.identifiers().localIdentifiers().asLocalIRI(entityKey, ctx.getEnvironment())
                        .flatMap(entityIdentifier -> api.entities().select().get(entityIdentifier, true, 0, ctx)),
                api.identifiers().prefixes().resolvePrefixedName(prefixedPoperty)
        ).map(pair -> {
            RdfFragment entity = pair.getT1();
            IRI property = pair.getT2();

            entity.reduce((st) -> {
                boolean isTypeDefinition = st.getSubject().equals(entity.getIdentifier()) && st.getPredicate().equals(RDF.TYPE);
                boolean isProperty = st.getPredicate().equals(property);
                return isTypeDefinition || isProperty;
            });

            return entity;
        });
    }
}
