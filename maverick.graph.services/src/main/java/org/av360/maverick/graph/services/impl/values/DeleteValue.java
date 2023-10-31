package org.av360.maverick.graph.services.impl.values;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.model.errors.requests.InvalidEntityUpdate;
import org.av360.maverick.graph.model.events.ValueRemovedEvent;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@Slf4j(topic = "graph.svc.value.del")
public class DeleteValue {
    private final ValueServicesImpl ctrl;


    public DeleteValue(ValueServicesImpl valueServices) {

        this.ctrl = valueServices;
    }

    public Mono<Transaction> remove(String entityKey, String predicate, String languageTag, String valueIdentifier, SessionContext ctx) {
        return Mono.zip(
                        ctrl.entityServices.resolveAndVerify(entityKey, ctx),
                        ctrl.schemaServices.resolvePrefixedName(predicate)
                )
                .switchIfEmpty(Mono.error(new InvalidEntityUpdate(entityKey, "Failed to remove literal")))
                .flatMap(tuple -> this.remove(tuple.getT1(), tuple.getT2(), languageTag, valueIdentifier, ctx));
    }

    public Mono<Transaction> remove(IRI entityIdentifier, IRI predicate, String languageTag, String valueIdentifier, SessionContext ctx) {
        return this.removeValueStatement(entityIdentifier, predicate, languageTag, valueIdentifier, new RdfTransaction(), ctx)
                .doOnSuccess(trx -> {
                    ctrl.eventPublisher.publishEvent(new ValueRemovedEvent(trx));
                });
    }


    /**
     * Deletes a value with a new transaction. Fails if no entity exists with the given subject
     */
    private Mono<Transaction> removeValueStatement(IRI entityIdentifier, IRI predicate, @Nullable String languageTag, @Nullable String valueIdentifier, Transaction transaction, SessionContext ctx) {
        return ctrl.entityServices.getStore(ctx).listStatements(entityIdentifier, predicate, null, ctx.getEnvironment())
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

                            if (object.isIRI()) {
                                return Mono.error(new InvalidEntityUpdate(entityIdentifier, "Invalid to remove links via the values api."));
                            } else if (object.isLiteral()) {
                                if (StringUtils.isNotBlank(valueIdentifier)) {
                                    String hash = ctrl.readValues.generateHashForValue(object.stringValue());
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
                        }

                        if (statementsToRemove.isEmpty() && StringUtils.isNotBlank(valueIdentifier)) {
                            return Mono.error(new InvalidEntityUpdate(entityIdentifier, "No value found with requested hash '%s'".formatted(valueIdentifier)));
                        }
                        if (statementsToRemove.isEmpty() && StringUtils.isNotBlank(languageTag)) {
                            return Mono.error(new InvalidEntityUpdate(entityIdentifier, "No value found with requested language tag '%s'".formatted(languageTag)));
                        }
                        if (statementsToRemove.size() > 1 && StringUtils.isNotBlank(languageTag)) {
                            return Mono.error(new InvalidEntityUpdate(entityIdentifier, "Multiple values found for language tag '%s'. Please delete by hash.".formatted(languageTag)));
                        }


                        return ctrl.entityServices.getStore(ctx).removeStatements(statementsToRemove, transaction);
                    } else {
                        return ctrl.entityServices.getStore(ctx).removeStatements(statements, transaction);
                    }

                })
                .flatMap(trx -> ctrl.entityServices.getStore(ctx).commit(trx, ctx.getEnvironment()));
    }
}
