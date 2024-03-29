package org.av360.maverick.graph.api.controller.entities;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.api.controller.AbstractController;
import org.av360.maverick.graph.api.controller.ValuesAPI;
import org.av360.maverick.graph.api.controller.dto.Responses;
import org.av360.maverick.graph.api.converter.dto.ValueObjectConverter;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.model.rdf.Triples;
import org.av360.maverick.graph.services.EntityServices;
import org.av360.maverick.graph.services.SchemaServices;
import org.av360.maverick.graph.services.ValueServices;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;

@RestController
@Slf4j(topic = "graph.ctrl.api.values")
public class ValuesController extends AbstractController implements ValuesAPI {


    protected final ValueServices values;
    protected final EntityServices entityServices;
    protected final SchemaServices schemaServices;

    public ValuesController(ValueServices values, EntityServices entities, SchemaServices schemaServices) {
        this.values = values;
        this.entityServices = entities;
        this.schemaServices = schemaServices;
    }

    @Override
    public Flux<AnnotatedStatement> list(String key,
                                         String prefixedProperty) {

        return super.acquireContext()
                .flatMap(ctx -> values.listValues(key, prefixedProperty, ctx))
                .flatMapIterable(Triples::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled())
                        log.debug("Request to list values of entity '{}'", key);
                });
    }

    @Override
    public Flux<Responses.ValueObject> listAsJson(String key, @Nullable String prefixedProperty) {
        /*
        TODO:
            - [ ] embed composites as json
         */

        return super.acquireContext()
                .flatMap(ctx -> values.listValues(key, prefixedProperty, ctx))
                .map(triples -> ValueObjectConverter.listFromTriples(key, triples))
                .flatMapMany(Flux::fromIterable)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled())
                        log.debug("Request to list values of entity '{}'", key);
                });
    }


    @Override
    public Flux<AnnotatedStatement> insertAsRdf(String key,
                                           String prefixedProperty,
                                           String value,
                                           String languageTag,
                                           Boolean replace) {
        Assert.isTrue(!value.matches("(?s).*[\\n\\r].*"), "Newlines in request body are not supported");

        return super.acquireContext()
                .flatMap(ctx -> values.insertValue(key, prefixedProperty, value, languageTag, replace, ctx))
                .flatMapIterable(Triples::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled())
                        log.debug("Request to set property '{}' of entity '{}' to value '{}'", prefixedProperty, key, value.length() > 64 ? value.substring(0, 64) : value);
                });
    }

    @Override
    public Flux<AnnotatedStatement> insertAsJson(String key, String prefixedProperty, String value, @Nullable String languageTag, @Nullable Boolean replace) {
        return null;
    }

    @Override
    public Flux<AnnotatedStatement> getAsRdf(String key, String prefixedProperty, @Nullable String languageTag, @Nullable String valueIdentifier) {
        return this.list(key, prefixedProperty);
    }

    @Override
    public Mono<Responses.ValueObject> getAsJson(String key, String prefixedProperty, @Nullable String languageTag,  @Nullable String valueIdentifier) {
        return super.acquireContext()
                .flatMap(ctx -> values.getValue(key, prefixedProperty, languageTag, valueIdentifier, ctx))
                .map(triples -> ValueObjectConverter.getFromTriples(key, triples))
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled())
                        log.debug("Request to list values of entity '{}'", key);
                });
    }


    @Override
    public Flux<AnnotatedStatement> remove(String key,
                                           String prefixedProperty,
                                           String languageTag,
                                           String valueIdentifier) {
        return super.acquireContext()
                .flatMap(ctx -> values.removeValue(key, prefixedProperty, languageTag, valueIdentifier, ctx))
                .flatMapIterable(Triples::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled()) log.debug("Delete property '{}' of entity '{}'", prefixedProperty, key);
                });
    }


    @Override
    public Flux<AnnotatedStatement> embed(@PathVariable String key, @PathVariable String prefixedProperty, @RequestBody Triples value) {

        return super.acquireContext()
                .flatMap(ctx ->
                        schemaServices.resolvePrefixedName(prefixedProperty)
                                .flatMap(predicate -> entityServices.linkEntityTo(key, predicate, value, ctx))
                )
                .flatMapIterable(Triples::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled())
                        log.debug("Request to add embedded entities as property '{}' to entity '{}'", prefixedProperty, key);
                });
    }


}

