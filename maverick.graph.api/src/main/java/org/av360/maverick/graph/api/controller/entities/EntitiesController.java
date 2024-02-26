package org.av360.maverick.graph.api.controller.entities;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.api.controller.AbstractController;
import org.av360.maverick.graph.api.controller.EntitiesAPI;
import org.av360.maverick.graph.api.controller.dto.Responses;
import org.av360.maverick.graph.api.converter.dto.EntityItemConverter;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.model.rdf.Triples;
import org.av360.maverick.graph.services.EntityServices;
import org.av360.maverick.graph.services.QueryServices;
import org.av360.maverick.graph.services.SchemaServices;
import org.av360.maverick.graph.store.rdf.fragments.TripleModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.Map;

@RestController
@Qualifier("EntityApi")
@Order(1)
@Slf4j(topic = "graph.ctrl.api.entities")
public class EntitiesController extends AbstractController implements EntitiesAPI {

    protected final ObjectMapper objectMapper;
    protected final EntityServices entityServices;
    protected final QueryServices queryServices;

    protected final SchemaServices schemaServices;

    public EntitiesController(ObjectMapper objectMapper, EntityServices graphService, QueryServices queryServices, SchemaServices schemaServices) {
        this.objectMapper = objectMapper;
        this.entityServices = graphService;
        this.queryServices = queryServices;
        this.schemaServices = schemaServices;
    }

    @Override
    public Flux<AnnotatedStatement> readAsRDF(@PathVariable String id, @RequestParam(required = false) @Nullable String property) {
        return super.acquireContext()
                .flatMap(ctx -> entityServices.find(id,  property, false, 0,  ctx))
                .flatMapIterable(TripleModel::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled()) log.debug("Request to read entity with id: {}", id);
                });
    }

    public Flux<AnnotatedStatement> readAsRDFStar(@PathVariable String id, @RequestParam(required = false) @Nullable String property) {
        return super.acquireContext()
                .flatMap(ctx -> entityServices.find(id,  property, true, 0,  ctx))
                .flatMapIterable(TripleModel::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled()) log.debug("Request to read entity including details with id: {}", id);
                });
    }

    @Override
    public Mono<Responses.EntityResponse> readAsItem(@PathVariable String id, @RequestParam(required = false) @Nullable String property) {
        return super.acquireContext()
                .flatMap(ctx -> entityServices.find(id,  property, true, 1,  ctx))
                .map(EntityItemConverter::convert)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled()) log.debug("Request to read entity including details with id: {}", id);
                });
    }

    @Override
    public Flux<AnnotatedStatement> list(
            @RequestParam(value = "limit", defaultValue = "100") Integer limit,
            @RequestParam(value = "offset", defaultValue = "0") Integer offset) {

        return super.acquireContext()
                .flatMapMany(ctx -> entityServices.list(limit, offset, ctx))
                .flatMapIterable(TripleModel::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled()) log.debug("Request to list entities");
                });
    }


    @Override
    public Flux<AnnotatedStatement> create(@RequestBody Triples request) {
        Assert.isTrue(request.getModel().size() > 0, "No statements in request detected.");

        return super.acquireContext()
                .flatMap(ctx -> entityServices.create(request, Map.of(), ctx))
                .flatMapIterable(Triples::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled()) log.debug("Request to create a new Entity");
                    if (log.isTraceEnabled()) log.trace("Payload: \n {}", request);
                });
    }




    @Override
    public Flux<AnnotatedStatement> delete(@PathVariable String id) {
        return super.acquireContext()
                .flatMap(ctx -> entityServices.remove(id, ctx))
                .flatMapIterable(Triples::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled()) log.debug("Delete an Entity");
                    if (log.isTraceEnabled()) log.trace("id: \n {}", id);
                });
    }

}
