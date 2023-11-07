package org.av360.maverick.graph.api.controller.entities;

import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.av360.maverick.graph.api.controller.AbstractController;
import org.av360.maverick.graph.api.controller.RelationsAPI;
import org.av360.maverick.graph.model.enums.RdfMimeTypes;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.model.rdf.Triples;
import org.av360.maverick.graph.services.EntityServices;
import org.av360.maverick.graph.services.SchemaServices;
import org.av360.maverick.graph.services.ValueServices;
import org.reactivestreams.Subscription;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;

@RestController
@Slf4j(topic = "graph.ctrl.api.relations")
public class RelationsController extends AbstractController implements RelationsAPI {


    protected final ValueServices values;

    protected final EntityServices entities;
    protected final SchemaServices schemaServices;

    public RelationsController(ValueServices values, EntityServices entities, SchemaServices schemaServices) {
        this.values = values;
        this.entities = entities;
        this.schemaServices = schemaServices;
    }

    @Override
    public Flux<AnnotatedStatement> list(@PathVariable String key) {
        return Flux.error(new NotImplementedException("Method has not been implemented yet."));
    }

    @Override
    public Flux<AnnotatedStatement> getLinksByType(@PathVariable String key, @PathVariable String prefixedProperty) {
        return super.acquireContext()
                .flatMap(ctx -> this.values.listRelations(key, prefixedProperty, ctx))
                .flatMapIterable(Triples::asStatements)
                .doOnSubscribe((Subscription s) -> {
                    if (log.isDebugEnabled())
                        log.debug("Request to get all '{}' links for entity '{}'", prefixedProperty, key);
                });

    }

    @Override
    public Flux<AnnotatedStatement> insert(@PathVariable String key, @PathVariable String prefixedProperty, @PathVariable String targetKey, @Nullable @RequestParam(required = false) Boolean replace) {
        return super.acquireContext()
                .flatMap(ctx -> this.values.insertLink(key, prefixedProperty, targetKey, replace, ctx))
                .flatMapIterable(Triples::asStatements)
                .doOnSubscribe((Subscription s) -> {
                    if (log.isDebugEnabled())
                        log.debug("Request to create link '{}' between entity '{}' and entity '{}'", prefixedProperty, key, targetKey);
                });

    }


    @Override
    public Flux<AnnotatedStatement> deleteLink(@PathVariable String key, @PathVariable String prefixedProperty, @PathVariable String targetKey) {
        return super.acquireContext()
                .flatMap(ctx -> this.values.removeLink(key, prefixedProperty, targetKey, ctx))
                .flatMap(Mono::just)
                .flatMapIterable(Triples::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled())
                        log.debug("Request to remove link '{}' between entity '{}' and entity '{}'", prefixedProperty, key, targetKey);
                })
                .doOnComplete(() -> {
                    if (log.isDebugEnabled())
                        log.debug("Request to remove link '{}' between entity '{}' and entity '{}' completed", prefixedProperty, key, targetKey);
                })
                .doOnError(error -> {
                    log.warn("Failed request to remove link '{}' between entity '{}' and entity '{}'", prefixedProperty, key, targetKey);
                });


    }


    @Override
    @Operation(summary = "Returns all links of the given type.", hidden = true)
    @PutMapping(value = "/entities/{key:[\\w|\\d|\\-|\\_]+}/links/{prefixedProperty:[\\w|\\d]+\\.[\\w|\\d]+}",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public Flux<AnnotatedStatement> batchCreateLinks(@PathVariable String key, @PathVariable String prefixedProperty) {
        return Flux.error(new NotImplementedException("Method has not been implemented yet."));
    }
}

