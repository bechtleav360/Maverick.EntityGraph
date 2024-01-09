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

package org.av360.maverick.graph.services.api.values.capabilities;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.errors.store.InvalidEntityModelException;
import org.av360.maverick.graph.model.rdf.Triples;
import org.av360.maverick.graph.model.vocabulary.Details;
import org.av360.maverick.graph.services.api.Api;
import org.av360.maverick.graph.services.api.values.ValuesUtils;
import org.av360.maverick.graph.store.rdf.fragments.RdfFragment;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j(topic = "graph.svc.value.read")
public class ReadValues {


    private final Api api;


    public ReadValues(Api api) {

        this.api = api;
    }

    public Mono<Triples> listValues(String entityKey, @Nullable String prefixedValuePredicate, SessionContext ctx) {

        return api.identifiers().localIdentifiers().asLocalIRI(entityKey, ctx.getEnvironment())
                .flatMap(entityIdentifier -> api.entities().select().get(entityIdentifier, true, 0, ctx))
                .flatMap(entity -> {
                    if(Objects.isNull(prefixedValuePredicate)) {
                        entity.reduce(statement -> statement.getObject().isLiteral());
                        return this.insertValueIdentifiers(entity);
                    } else {
                        return this.api.identifiers().prefixes().resolvePrefixedName(prefixedValuePredicate)
                                .map(valuePredicate -> entity.filter(st -> {
                                    if(st.getSubject().isTriple()) {
                                        // include details for the given value predicate
                                        return ((Triple) st.getSubject()).getPredicate().equals(valuePredicate);
                                    } else {
                                        // include the value statements
                                        return st.getPredicate().equals(valuePredicate);
                                    }
                                }))
                                .flatMap(triples -> this.insertValueIdentifiers(triples));
                    }
                });
    }






    private Mono<Triples> insertValueIdentifiers(Triples entity) {
        new HashSet<>(entity.getModel())
                .stream().filter(statement -> statement.getSubject().isIRI())
                .forEach(statement -> {
                    Triple triple = Values.triple(statement);
                    String hash = ValuesUtils.generateHashForValue(statement.getPredicate().stringValue(), statement.getObject().stringValue());
                    entity.getModel().add(triple, Details.HASH, Values.literal(hash));
                });
        return Mono.just(entity);
    }




    public Optional<Triple> findValueTripleByLanguageTag(RdfFragment entity, IRI valuePredicate, String languageTag) {
        return entity.streamValues(entity.getIdentifier(), valuePredicate)
                .filter(Value::isLiteral)
                .filter(literal -> ((Literal) literal).getLanguage().map(tag -> tag.equalsIgnoreCase(languageTag)).orElseGet(() -> Boolean.FALSE))
                .map(requestedLiteral -> Values.triple(entity.getIdentifier(), valuePredicate, requestedLiteral))
                .findFirst();

    }


    public Optional<Triple> findValueTripleByHash(RdfFragment entity, IRI valuePredicate, String hash) {
        return entity.streamValues(entity.getIdentifier(), valuePredicate)
                .filter(literal -> {
                    String generatedHash = ValuesUtils.generateHashForValue(valuePredicate.stringValue(), literal.stringValue());
                    return hash.equalsIgnoreCase(generatedHash);
                })
                .map(requestedLiteral -> Values.triple(entity.getIdentifier(), valuePredicate, requestedLiteral))
                .findFirst();
    }

    public Optional<Triple> findSingleValueTriple(RdfFragment entity, IRI valuePredicate) throws InvalidEntityModelException {
        List<Triple> list = entity.streamValues(entity.getIdentifier(), valuePredicate)
                .filter(Value::isLiteral)
                .map(requestedLiteral -> Values.triple(entity.getIdentifier(), valuePredicate, requestedLiteral))
                .toList();
        if(list.size() > 1) throw new InvalidEntityModelException("Multiple value for entity with key '%s' and value '%s' found.".formatted(entity.getIdentifier(), valuePredicate));
        else if(list.size() == 0) return Optional.empty();
        else return Optional.of(list.get(0));
    }






}
