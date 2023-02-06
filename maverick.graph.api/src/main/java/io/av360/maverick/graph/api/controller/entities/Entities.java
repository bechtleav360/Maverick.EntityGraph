package io.av360.maverick.graph.api.controller.entities;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.av360.maverick.graph.api.controller.AbstractController;
import io.av360.maverick.graph.model.enums.RdfMimeTypes;
import io.av360.maverick.graph.model.rdf.GeneratedIdentifier;
import io.av360.maverick.graph.model.rdf.NamespaceAwareStatement;
import io.av360.maverick.graph.services.EntityServices;
import io.av360.maverick.graph.services.impl.QueryServicesImpl;
import io.av360.maverick.graph.store.rdf.models.TripleBag;
import io.av360.maverick.graph.store.rdf.models.TripleModel;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import javax.annotation.Nullable;
import java.util.Map;

@RestController
@RequestMapping(path = "/api/entities")
@Slf4j(topic = "graph.api.entities")
@OpenAPIDefinition(

)
@SecurityRequirement(name = "api_key")
public class Entities extends AbstractController {

    protected final ObjectMapper objectMapper;
    protected final EntityServices entityServices;
    protected final QueryServicesImpl queryServices;

    public Entities(ObjectMapper objectMapper, EntityServices graphService, QueryServicesImpl queryServices) {
        this.objectMapper = objectMapper;
        this.entityServices = graphService;
        this.queryServices = queryServices;
    }

    @Operation(summary = "Returns an entity with the given unique identifier. ")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "404", description = "Entity with the given identifier does not exist", content = { @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorAttributes.class))})
    })
    @GetMapping(value = "/{id}",
            produces = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> read(@PathVariable String id, @RequestParam(required = false) @Nullable String property) {
        if(StringUtils.isBlank(property)) {
            Assert.isTrue(id.length() == GeneratedIdentifier.LENGTH, "Incorrect length for identifier.");

            return super.getAuthentication()
                    .flatMap(authentication -> entityServices.readEntity(id, authentication))
                    .flatMapIterable(TripleModel::asStatements)
                    .doOnSubscribe(s -> {
                        if (log.isDebugEnabled()) log.debug("Request to read entity with id: {}", id);
                    });
        } else {
            String[] split = splitPrefixedIdentifier(property);

            return super.getAuthentication()
                    .flatMap(authentication -> queryServices.findEntityByProperty(id, split[0], split[1], authentication))
                    .flatMapIterable(TripleModel::asStatements)
                    .doOnSubscribe(s -> {
                        if (log.isDebugEnabled()) log.debug("Request to read entity with id: {}", id);
                    });
        }


    }

    @GetMapping(value = "", produces = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> list(
            @RequestParam(value = "limit", defaultValue = "5000") Integer limit,
            @RequestParam(value = "offset", defaultValue = "0") Integer offset) {

        return super.getAuthentication()
                .flatMapMany(authentication -> queryServices.listEntities(authentication, limit, offset))
                .flatMapIterable(TripleModel::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled()) log.debug("Request to list entities");
                });
    }




    @PostMapping(value = "",
            consumes = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE},
            produces = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE})
    @ResponseStatus(HttpStatus.ACCEPTED)
    Flux<NamespaceAwareStatement> create(@RequestBody TripleBag request) {
        Assert.isTrue(request.getModel().size() > 0, "No statements in request detected.");

        return super.getAuthentication()
                .flatMap(authentication -> entityServices.createEntity(request, Map.of(), authentication))
                .flatMapIterable(TripleModel::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled()) log.debug("Request to create a new Entity");
                    if (log.isTraceEnabled()) log.trace("Payload: \n {}", request.toString());
                });
    }

    @PostMapping(value = "/{id:[\\w|\\d|-|_]+}/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}",
            consumes = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE},
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.CREATED)
    Flux<NamespaceAwareStatement> embed(@PathVariable String id, @PathVariable String prefixedKey, @RequestBody TripleBag value) {

        String[] property = splitPrefixedIdentifier(prefixedKey);
        return super.getAuthentication()
                .flatMap(authentication -> entityServices.linkEntityTo(id, property[0], property[1], value, authentication))
                .flatMapIterable(TripleModel::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled())
                        log.debug("Request to add embedded entities as property '{}' to entity '{}'", prefixedKey, id);
                });
    }


    @DeleteMapping(value = "/{id:[\\w|\\d|-|_]+}",
            produces = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> delete(@PathVariable String id) {
        Assert.isTrue(id.length() == GeneratedIdentifier.LENGTH, "Incorrect length for identifier.");

        return super.getAuthentication()
                .flatMap(authentication -> entityServices.deleteEntity(id, authentication))
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
