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
import org.av360.maverick.graph.services.api.Api;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

@Slf4j
public class PrefixResolver {
    private final Api api;

    public PrefixResolver(Api services) {

        this.api = services;
    }


    public Mono<IRI> resolvePrefixedName(String prefixedName) {
        String[] property = splitPrefixedIdentifier(prefixedName);

        return this.api.schema().namespaces().getNamespaceFor(property[0])
                .map(namespace -> SimpleValueFactory.getInstance().createIRI(namespace.getName(), property[1]))
                .doOnError(err -> log.warn("Failed to resolve property {} due to error: '{}': ", prefixedName, err.getMessage()))
                .doOnSuccess(iri -> log.trace("Resolved property {} to qualified name '{}'", prefixedName, iri));
    }

    protected String[] splitPrefixedIdentifier(String prefixedKey) {
        String[] property = prefixedKey.split("\\.");
        Assert.isTrue(property.length == 2, "Failed to extract prefix and label from path parameter " + prefixedKey);
        return property;
    }
}
