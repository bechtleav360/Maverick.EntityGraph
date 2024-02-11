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
import org.apache.commons.lang3.StringUtils;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.model.errors.requests.InvalidEntityUpdate;
import org.av360.maverick.graph.model.events.ValueRemovedEvent;
import org.av360.maverick.graph.services.api.Api;
import org.av360.maverick.graph.services.api.values.ValuesUtils;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@Slf4j(topic = "graph.svc.value.remove")
public class DeleteValue {
    private final Api api;


    public DeleteValue(Api ctrl) {
        this.api = ctrl;
    }

    public Mono<Transaction> remove(String entityKey, String predicate, String languageTag, String valueIdentifier, SessionContext ctx) {
        return Mono.zip(
                        api.entities().select().resolveAndVerify(entityKey, ctx),
                        api.identifiers().prefixes().resolvePrefixedName(predicate)
                )
                .switchIfEmpty(Mono.error(new InvalidEntityUpdate(entityKey, "Failed to remove literal")))
                .flatMap(tuple -> this.remove(tuple.getT1(), tuple.getT2(), languageTag, valueIdentifier, ctx));
    }

    public Mono<Transaction> remove(IRI entityIdentifier, IRI predicate, String languageTag, String valueIdentifier, SessionContext ctx) {

        return this.removeValueStatements(entityIdentifier, predicate, languageTag, valueIdentifier, new RdfTransaction(), ctx)
                .flatMap(trx -> api.details().removes().removeAllDetails(entityIdentifier, predicate, languageTag, valueIdentifier, trx, ctx))
                .flatMap(trx -> api.entities().getStore().asCommitable().commit(trx, ctx.getEnvironment()))
                //.flatMap(trx -> ser.entityServices.getStore(ctx).asCommitable().commit(trx, ctx.getEnvironment()))
                .doOnSuccess(trx -> {
                    api.publishEvent(new ValueRemovedEvent(trx, ctx.getEnvironment()));
                });
    }


    /**
     * Deletes a value with a new transaction. Fails if no entity exists with the given subject
     */
    private Mono<Transaction> removeValueStatements(IRI entityIdentifier, IRI predicate, @Nullable String languageTag, @Nullable String valueIdentifier, Transaction transaction, SessionContext ctx) {
        return api.entities().getStore().asStatementsAware().listStatements(entityIdentifier, predicate, null, ctx.getEnvironment())
                .flatMap(statements -> {
                    if (statements.size() > 1) {
                        List<Statement> statementsToRemove = new ArrayList<>();

                        if (StringUtils.isEmpty(languageTag) && StringUtils.isEmpty(valueIdentifier)) {
                            log.error("Failed to identify unique statement for predicate {} to remove for entity {}.", predicate.getLocalName(), entityIdentifier.getLocalName());
                            statements.forEach(st -> log.trace("Candidate: {} - {} ", st.getPredicate(), st.getObject()));
                            return Mono.error(new InvalidEntityUpdate(entityIdentifier, "Multiple values for given predicate detected, but no language tag or hash identifier in request."));
                        }

                        for (Statement st : statements) {
                            Value object = st.getObject();
                            if (StringUtils.isNotBlank(valueIdentifier)) {
                                String hash = ValuesUtils.generateHashForValue(predicate.stringValue(), object.stringValue());
                                if (hash.equalsIgnoreCase(valueIdentifier)) {
                                    statementsToRemove.add(st);
                                }
                            } else if (StringUtils.isNotBlank(languageTag)) {
                                Literal currentLiteral = (Literal) object;
                                if (StringUtils.equals(currentLiteral.getLanguage().orElse("invalid"), languageTag)) {
                                    statementsToRemove.add(st);
                                }
                            }
                        }

                        if (statementsToRemove.isEmpty() && StringUtils.isNotBlank(valueIdentifier)) {
                            return Mono.error(new InvalidEntityUpdate(entityIdentifier, "No value found with requested value identifier '%s'".formatted(valueIdentifier)));
                        }
                        if (statementsToRemove.isEmpty() && StringUtils.isNotBlank(languageTag)) {
                            return Mono.error(new InvalidEntityUpdate(entityIdentifier, "No value found with requested language tag '%s'".formatted(languageTag)));
                        }
                        if (statementsToRemove.size() > 1 && StringUtils.isNotBlank(languageTag)) {
                            return Mono.error(new InvalidEntityUpdate(entityIdentifier, "Multiple values found for language tag '%s'. Please delete by value identifier.".formatted(languageTag)));
                        }

                        return Mono.just(transaction.removes(statementsToRemove));
                    } else {
                        return Mono.just(transaction.removes(statements));
                    }

                });
    }
}
