package org.av360.maverick.graph.services.impl.values;

import org.apache.commons.lang3.StringUtils;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.model.errors.requests.InvalidEntityUpdate;
import org.av360.maverick.graph.model.events.ValueInsertedEvent;
import org.av360.maverick.graph.model.events.ValueReplacedEvent;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.LanguageHandler;
import org.eclipse.rdf4j.rio.LanguageHandlerRegistry;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;

public class InsertValues {

    private final ValueServicesImpl ctrl;

    public InsertValues(ValueServicesImpl valueServices) {
        this.ctrl = valueServices;
    }

    public Mono<Transaction> insert(String entityKey, String prefixedPoperty, String value, String languageTag, Boolean replace, SessionContext ctx) {
        return Mono.zip(
                        ctrl.entityServices.resolveAndVerify(entityKey, ctx),
                        ctrl.schemaServices.resolvePrefixedName(prefixedPoperty),
                        this.normalizeValue(value, languageTag)
                )
                .switchIfEmpty(Mono.error(new InvalidEntityUpdate(entityKey, "Failed to insert value")))
                .flatMap(triple -> this.insert(triple.getT1(), triple.getT2(), triple.getT3(), Objects.isNull(replace) ? Boolean.FALSE : replace, ctx));
    }


    public Mono<Transaction> insert(IRI entityIdentifier, IRI predicate, Value value, @Nullable Boolean replace, SessionContext ctx) {
        return ctrl.insertStatement(entityIdentifier, predicate, value, new RdfTransaction(), !Objects.isNull(replace) && replace, ctx)
                .doOnSuccess(trx -> {
                    ctrl.eventPublisher.publishEvent(new ValueInsertedEvent(trx));
                });
    }


    private Mono<Value> normalizeValue(String value, String languageTag) {
        if (value.matches("^<\\w+:(/?/?)[^\\s>]+>$")) {
            value = value.substring(1, value.length() - 1);
            return Mono.just(SimpleValueFactory.getInstance().createIRI(value));
        } else if (value.matches("^\\w+:(/?/?)[^\\s>]+$")) {
            return Mono.just(SimpleValueFactory.getInstance().createLiteral(value));
        } else

            return this.extractLanguageTag(value, languageTag);
    }


    /**
     * Extracts the language tag.
     * Rules:
     * - tag in request parameter > tag in value
     * - default lang tag is "en"
     * - if one is invalid, we take the other (and write a warning into the logs)
     *
     * @param value    the literal value as string
     * @param paramTag the request parameter for the language tag, can be null
     * @return the identified language tag
     */
    private Mono<Value> extractLanguageTag(String value, @Nullable String paramTag) {
        return Mono.create(sink -> {
            LanguageHandler languageHandler = LanguageHandlerRegistry.getInstance().get(LanguageHandler.BCP47).orElseThrow();
            SimpleValueFactory svf = SimpleValueFactory.getInstance();

            String valueTag = value.matches(".*@\\w\\w-?[\\w\\d-]*$") ? value.substring(value.lastIndexOf('@') + 1) : "";
            String strippedValue = StringUtils.isNotBlank(valueTag) ? value.substring(0, value.lastIndexOf('@')) : value;

            if (StringUtils.isNotBlank(paramTag)) {
                if (languageHandler.isRecognizedLanguage(paramTag) && languageHandler.verifyLanguage(value, paramTag)) {
                    sink.success(languageHandler.normalizeLanguage(strippedValue, paramTag, svf));
                } else {
                    sink.error(new IOException("Invalid language tag in parameter: " + paramTag));
                }
            } else if (StringUtils.isNotBlank(valueTag)) {
                if (languageHandler.isRecognizedLanguage(valueTag) && languageHandler.verifyLanguage(value, valueTag)) {
                    sink.success(languageHandler.normalizeLanguage(strippedValue, valueTag, svf));
                } else {
                    sink.error(new IOException("Invalid language tag in value: " + valueTag));
                }
            } else {
                sink.success(languageHandler.normalizeLanguage(strippedValue, "en", svf));
            }

        });
    }

    public Mono<Transaction> insertComposite(IRI entityIdentifier, IRI predicate, Resource embeddedNode, Set<Statement> embedded, SessionContext ctx) {
        return ctrl.insertStatements(entityIdentifier, predicate, embeddedNode, embedded, new RdfTransaction(), ctx)
                .doOnSuccess(trx -> {
                    ctrl.eventPublisher.publishEvent(new ValueInsertedEvent(trx));
                });
    }

    public Mono<Transaction> replace(IRI entityIdentifier, IRI predicate, Value oldValue, Value newValue, SessionContext ctx) {
        return ctrl.entityServices.getStore(ctx).asCommitable().markForRemoval(entityIdentifier, predicate, oldValue, new RdfTransaction())
                .map(transaction -> transaction.inserts(entityIdentifier, predicate, newValue))
                .flatMap(trx -> ctrl.entityServices.getStore(ctx).asCommitable().commit(trx, ctx.getEnvironment()))
                .doOnSuccess(trx -> {
                    ctrl.eventPublisher.publishEvent(new ValueReplacedEvent(trx));
                });
    }
}
