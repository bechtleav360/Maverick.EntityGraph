package org.av360.maverick.graph.api.controller.entities;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.api.controller.AbstractController;
import org.av360.maverick.graph.model.api.EntitiesAPI;
import org.av360.maverick.graph.model.enums.RdfMimeTypes;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.model.rdf.Triples;
import org.av360.maverick.graph.services.EntityServices;
import org.av360.maverick.graph.services.QueryServices;
import org.av360.maverick.graph.services.SchemaServices;
import org.av360.maverick.graph.store.rdf.fragments.TripleModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import javax.annotation.Nullable;
import java.util.Map;

@RestController
@Qualifier("EntityApi")
@Order(1)
@RequestMapping(path = "/api")
@Slf4j(topic = "graph.ctrl.api.entities")
@OpenAPIDefinition(

)
@SecurityRequirement(name = "api_key")
@Tag(name = "Entities")
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
    @Operation(summary = "Returns an entity with the given unique identifier. ")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "404", description = "Entity with the given identifier does not exist", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ErrorAttributes.class))})
    })
    @GetMapping(value = "/entities/{id}",
            produces = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public Flux<AnnotatedStatement> read(@PathVariable String id, @RequestParam(required = false) @Nullable String property) {

        return super.getAuthentication()
                .flatMap(authentication -> entityServices.find(id, property, authentication))
                .flatMapIterable(TripleModel::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled()) log.debug("Request to read entity with id: {}", id);
                });


    }

    @Override
    @GetMapping(value = "/entities", produces = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public Flux<AnnotatedStatement> list(
            @RequestParam(value = "limit", defaultValue = "100") Integer limit,
            @RequestParam(value = "offset", defaultValue = "0") Integer offset) {

        return super.getAuthentication()
                .flatMapMany(authentication -> entityServices.list(authentication, limit, offset))
                .flatMapIterable(TripleModel::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled()) log.debug("Request to list entities");
                });
    }


    @Override
    @PostMapping(value = "/entities",
            consumes = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE},
            produces = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE})
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Flux<AnnotatedStatement> create(@RequestBody Triples request) {
        Assert.isTrue(request.getModel().size() > 0, "No statements in request detected.");

        return super.getAuthentication()
                .flatMap(authentication -> entityServices.create(request, Map.of(), authentication))
                .flatMapIterable(TripleModel::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled()) log.debug("Request to create a new Entity");
                    if (log.isTraceEnabled()) log.trace("Payload: \n {}", request);
                });
    }

    @Override
    @PostMapping(value = "/entities/{id:[\\w|\\d|\\-|\\_]+}/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}",
            consumes = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE},
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.CREATED)
    public Flux<AnnotatedStatement> embed(@PathVariable String id, @PathVariable String prefixedKey, @RequestBody Triples value) {

        return super.getAuthentication()
                .flatMap(authentication ->
                        schemaServices.resolvePrefixedName(prefixedKey)
                                .flatMap(predicate -> entityServices.linkEntityTo(id, predicate, value, authentication))
                )
                .flatMapIterable(TripleModel::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled())
                        log.debug("Request to add embedded entities as property '{}' to entity '{}'", prefixedKey, id);
                });
    }


    @Override
    @DeleteMapping(value = "/entities/{id:[\\w|\\d|\\-|\\_]+}",
            produces = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public Flux<AnnotatedStatement> delete(@PathVariable String id) {
        return super.getAuthentication()
                .flatMap(authentication -> entityServices.remove(id, authentication))
                .flatMapIterable(TripleModel::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled()) log.debug("Delete an Entity");
                    if (log.isTraceEnabled()) log.trace("id: \n {}", id);
                });
    }




    /* //
    @ApiOperation(value = "Update entity", tags = {"v3", "entity"})
    @PutMapping(value = "",
                consumes = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE},
                produces = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE})
        @ResponseStatus(HttpStatus.ACCEPTED)
        Flux<NamespaceAwareStatement> updateValue(@RequestBody Incoming request) {
            Assert.isTrue(request.getModel().size() > 0, "No statements in request detected.");

            return super.getAuthentication()
                    .flatMap(authentication ->  entityServices.createEntity(request, Map.of(), authentication))
                    .flatMapIterable(AbstractModel::asStatements)
                    .doOnSubscribe(s -> {
                        if (log.isDebugEnabled()) log.debug("Create a new Entity");
                        if (log.isTraceEnabled()) log.trace("Payload: \n {}", request.toString());
                    });
        }
    */

    /*
    @ApiOperation(value = "Read entity with type coercion", tags = {"v4", "entity"})
    @GetMapping("/{prefixedType:[\\w|\\d]+\\.[\\w|\\d]+}/{id:[\\w|\\d|-]+}")
    @ResponseStatus(HttpStatus.OK)
    Mono<ServerResponse> readWithType(@PathVariable String prefixedType, @PathVariable String id);

    @ApiOperation(value = "Read entity by example", tags = {"v1", "entity"})
    @PostMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    Mono<ServerResponse> readByExample(@RequestBody ObjectNode frame);

    @ApiOperation(value = "List entities by type", tags = {"v2", "entity"})
    @GetMapping("/{prefixedType:[\\w|\\d]+\\.[\\w|\\d]+}")
    @ResponseStatus(HttpStatus.OK)
    Mono<ServerResponse> listEntities(@RequestParam String page, @RequestParam int count);

    @ApiOperation(value = "Create entity with type", tags = {"v3", "entity"})
    @PostMapping("/{prefixedType}")
    @ResponseStatus(HttpStatus.CREATED)
    Mono<ServerResponse> createEntityWithType(@RequestBody String frame);

    @ApiOperation(value = "Delete entity", tags = {"v2", "entity"})
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    Mono<ServerResponse> deleteEntity(@PathVariable String id);

    @ApiOperation(value = "Update entity", tags = {"v3", "entity"})
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    Mono<ServerResponse> patchEntity(@PathVariable String id);

    @ApiOperation(value = "Update value", tags = {"v3", "entity"})
    @PutMapping("/{id}/{prefixedKey}")
    @ResponseStatus(HttpStatus.OK)
    Mono<ServerResponse> updateValue(@PathVariable String id, @PathVariable String prefixedKey);

    @ApiOperation(value = "Read value or embedded object", tags = {"v2", "entity"})
    @GetMapping("/{id}/{prefixedKey}")
    @ResponseStatus(HttpStatus.OK)
    Mono<ServerResponse> readValue(@PathVariable String id, @PathVariable String prefixedKey);


    @ApiOperation(value = "Annotate value or relation", tags = {"v2", "entity"})
    @PutMapping("/{id}/{prefixedKey}")
    @ResponseStatus(HttpStatus.CREATED)
    Mono<ServerResponse> addAnnotations(@PathVariable String id, @PathVariable String prefixedKey);

    @ApiOperation(value = "Delete value or relation", tags = {"v2", "entity"})
    @DeleteMapping("/{id}/{prefixedKey}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    Mono<ServerResponse> deleteValue(@PathVariable String id, @PathVariable String prefixedKey);

     */
}
