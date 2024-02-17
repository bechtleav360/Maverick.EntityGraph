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

package org.av360.maverick.graph.services.api.details.capabilities;

import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.model.errors.InconsistentModelException;
import org.av360.maverick.graph.model.errors.requests.InvalidEntityUpdate;
import org.av360.maverick.graph.model.errors.store.InvalidEntityModelException;
import org.av360.maverick.graph.model.events.DetailInsertedEvent;
import org.av360.maverick.graph.services.api.Api;
import org.av360.maverick.graph.store.rdf.fragments.RdfFragment;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Statements;
import org.eclipse.rdf4j.model.util.Values;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Optional;

public class InsertDetails {
    private final Api api;

    public InsertDetails(Api valueServices) {
        this.api = valueServices;
    }



    public Mono<Transaction> insert(IRI entityIdentifier, IRI valuePredicate, IRI detailPredicate, String detailValue, String valueIdentifier, SessionContext ctx) {
        return api.entities().select().get(entityIdentifier, true, 0, ctx)
                .flatMap(entityFragment -> {
                    if (Objects.isNull(valueIdentifier)) {
                        return this.insertWithoutHash(entityFragment, valuePredicate, detailPredicate, detailValue, ctx);
                    } else {
                        return this.insertWithHash(entityFragment, valuePredicate, detailPredicate, detailValue, valueIdentifier, ctx);
                    }
                }).doOnSuccess(trx -> {
                    api.publishEvent(new DetailInsertedEvent(trx, ctx.getEnvironment()));
                });
    }

    private Mono<Transaction> insertWithHash(RdfFragment entity, IRI valuePredicate, IRI detailPredicate, String value, String hash, SessionContext ctx) {
        return this.buildDetailStatementForValueWithHash(entity, valuePredicate, detailPredicate, value, hash)
                .map(statement -> {
                    // check if we already have a statement
                    api.details().selects().hasDetail((IRI) statement.getSubject(), valuePredicate, detailPredicate);


                    return new RdfTransaction().forInsert(statement);

                })

                .flatMap(trx -> api.entities().commit(trx, ctx.getEnvironment()));
    }


    private Mono<Transaction> insertWithoutHash(RdfFragment entity, IRI valuePredicate, IRI detailPredicate, String value, SessionContext ctx) {

        try {
            Optional<Value> distinctValue = entity.findDistinctValue(entity.getIdentifier(), valuePredicate);

            if (distinctValue.isEmpty()) {
                return Mono.error(new InvalidEntityUpdate(entity.getIdentifier(), "No value exists for predicate %s".formatted(valuePredicate)));
            }

            Triple triple = Values.triple(entity.getIdentifier(), valuePredicate, distinctValue.get());
            Statement insertStatement = Statements.statement(triple, detailPredicate, Values.literal(value), null);

            Transaction trx = new RdfTransaction()
                    .forInsert(insertStatement)
                    .removes(entity.listStatements(triple, detailPredicate, null));
            return api.entities().commit(trx, ctx.getEnvironment());


        } catch (InconsistentModelException e) {
            return Mono.error(new InvalidEntityUpdate(entity.getIdentifier(), "Multiple values for property %s. Use the hash identifier to select the value to update.".formatted(valuePredicate)));
        }
    }


    Mono<Statement> buildDetailStatementForValueWithHash(RdfFragment entity, IRI valuePredicate, IRI detailPredicate, String value, String valueHash) {
        Optional<Triple> requestedTriple =  this.api.values().read().findValueTripleByHash(entity, valuePredicate, valueHash);

        if (requestedTriple.isEmpty())
            return Mono.error(new InvalidEntityUpdate(entity.getIdentifier(), "No value exists in entity <%s> for predicate <%s> and hash '%s'".formatted(entity.getIdentifier(), valuePredicate, valueHash)));

        Statement annotationStatement = Statements.statement(requestedTriple.get(), detailPredicate, Values.literal(value), null);
        return Mono.just(annotationStatement);
    }

    Mono<Statement> buildDetailStatementForSingleValue(RdfFragment entity, IRI valuePredicate, IRI detailPredicate) {
        try {
            Optional<Triple> requestedTriple =  this.api.values().read().findSingleValueTriple(entity, valuePredicate);
            if (requestedTriple.isEmpty())
                return Mono.error(new InvalidEntityUpdate(entity.getIdentifier(), "No value exists in entity <%s> for predicate <%s>".formatted(entity.getIdentifier(), valuePredicate)));

            Statement annotationStatement = Statements.statement(requestedTriple.get(), detailPredicate, null, null);
            return Mono.just(annotationStatement);
        } catch (InvalidEntityModelException e) {
            return Mono.error(e);
        }

    }


}
