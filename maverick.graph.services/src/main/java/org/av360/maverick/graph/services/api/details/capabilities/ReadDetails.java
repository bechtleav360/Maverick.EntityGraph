/*
 * Copyright (c) 2023-2024.
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

package org.av360.maverick.graph.services.api.details.capabilities;

import org.apache.commons.lang3.tuple.Pair;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.errors.requests.DetailNotFound;
import org.av360.maverick.graph.model.vocabulary.Details;
import org.av360.maverick.graph.services.api.Api;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

public class ReadDetails {
    private final Api services;

    public ReadDetails(Api ctrl) {

        this.services = ctrl;
    }

    public Flux<Pair<IRI, Value>> listDetails(String entityKey, String prefixedProperty, String valueIdentifier, SessionContext ctx) {
        return this.services.values().read().listValues(entityKey, prefixedProperty, ctx)
                .flatMapMany(triples -> {
                    Literal valueIdentifierLiteral = Values.literal(valueIdentifier);

                    Optional<Statement> detailsHashStatement = triples.findStatement(null, Details.HASH, valueIdentifierLiteral);
                    if(detailsHashStatement.isEmpty()) return Flux.empty();

                    Iterable<Statement> statements = triples.getModel().getStatements(detailsHashStatement.get().getSubject(), null, null);

                    return Flux.fromIterable(statements);
                })
                .switchIfEmpty(Flux.error(new DetailNotFound(entityKey, prefixedProperty, valueIdentifier)))
                .map(statement -> {
                    return Pair.of(statement.getPredicate(), statement.getObject());
                });
    }

    public Mono<Boolean> hasDetail(IRI entityIdentifier, IRI valuePredicate, IRI detailPredicate) {
        return Mono.just(Boolean.FALSE);
    }
}
