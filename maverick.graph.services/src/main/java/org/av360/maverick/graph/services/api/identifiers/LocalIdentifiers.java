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

package org.av360.maverick.graph.services.api.identifiers;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.rdf.LocalIRI;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.av360.maverick.graph.services.IdentifierServices;
import org.av360.maverick.graph.services.api.Api;
import org.eclipse.rdf4j.model.IRI;
import reactor.core.publisher.Mono;

@Slf4j
public class LocalIdentifiers {
    private final Api services;
    private final IdentifierServices identifierServices;

    public LocalIdentifiers(Api services, IdentifierServices identifierServices) {

        this.services = services;
        this.identifierServices = identifierServices;
    }

    public Mono<IRI> asLocalIRI(String key, Environment environment) {
        return this.asLocalIRI(key, Local.Entities.NAME, environment);
    }

    public Mono<IRI> asLocalIRI(String key, String namespace, Environment environment) {
        String checkedKey = this.identifierServices.validate(key, environment);
        return Mono.just(buildLocalIri(namespace, checkedKey));
    }

    public IRI buildLocalIri(String namespace, String key) {
        return LocalIRI.from(namespace, key);
    }
}
