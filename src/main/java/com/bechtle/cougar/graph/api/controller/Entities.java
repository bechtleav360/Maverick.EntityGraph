package com.bechtle.cougar.graph.api.controller;

import com.bechtle.cougar.graph.domain.model.enums.RdfMimeTypes;
import com.bechtle.cougar.graph.domain.model.extensions.GeneratedIdentifier;
import com.bechtle.cougar.graph.domain.model.extensions.NamespaceAwareStatement;
import com.bechtle.cougar.graph.domain.model.wrapper.AbstractModel;
import com.bechtle.cougar.graph.domain.model.wrapper.Incoming;
import com.bechtle.cougar.graph.domain.services.EntityServices;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping(path = "/api/entities")
@Api(tags = "Entities")
@Slf4j(topic = "cougar.graph.api")
public class Entities {

    protected final ObjectMapper objectMapper;
    protected final EntityServices entityServices;

    public Entities(ObjectMapper objectMapper, EntityServices graphService) {
        this.objectMapper = objectMapper;
        this.entityServices = graphService;
    }

    @ApiOperation(value = "Read entity", tags = {"v1"})
    @GetMapping(value = "/{id:[\\w|\\d|-|_]+}", produces = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> read(@PathVariable String id) {
        log.trace("(Request) Reading Entity with id: {}", id);
        Assert.isTrue(id.length() == GeneratedIdentifier.LENGTH, "Incorrect length for identifier.");

        return entityServices.readEntity(id)
                .flatMapIterable(AbstractModel::asStatements);
    }

    @ApiOperation(value = "Create entity", tags = {"v1"})
    @PostMapping(value = "",
            consumes = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE},
            produces = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE})
    @ResponseStatus(HttpStatus.ACCEPTED)
    Flux<NamespaceAwareStatement> createEntity(@RequestBody Incoming request) {

        if (log.isDebugEnabled()) log.debug("(Request) Create a new Entity");
        if (log.isTraceEnabled()) log.trace("(Request) Payload: \n {}", request.toString());

        Assert.isTrue(request.getModel().size() > 0, "No statements in request detected.");
        Map<String, String> parameter = Map.of();
        return entityServices.createEntity(request, parameter).flatMapIterable(AbstractModel::asStatements);
    }


    @ApiOperation(value = "Create value or relation", tags = {"v1"})
    @PostMapping(value = "/{id:[\\w|\\d|-|_]+}/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.CREATED)
    Flux<NamespaceAwareStatement> createValue(@PathVariable String id, @PathVariable String prefixedKey, @RequestBody String value) {

        if (log.isDebugEnabled())
            log.debug("(Request) Set property '{}' of entity '{}' to value '{}'", prefixedKey, id, value.length() > 64 ? value.substring(0, 64) : value);

        Assert.isTrue(!value.matches("(?s).*[\\n\\r].*"), "Newlines in request body are not supported");

        String[] property = splitPrefixedIdentifier(prefixedKey);

        return entityServices.setValue(id, property[0], property[1], value)
                .map(transaction -> {
                    return transaction;
                })
                .flatMapIterable(AbstractModel::asStatements);

    }

    @ApiOperation(value = "Create value or relation", tags = {"v1"})
    @PostMapping(value = "/{id:[\\w|\\d|-|_]+}/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}",
            consumes = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE},
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.CREATED)
    Flux<NamespaceAwareStatement> createEmbedded(@PathVariable String id, @PathVariable String prefixedKey, @RequestBody Incoming value) {

        if (log.isDebugEnabled())
            log.debug("(Request) Add embedded entity as property '{}' to entity '{}'", prefixedKey, id);

        String[] property = splitPrefixedIdentifier(prefixedKey);

        return entityServices.link(id, property[0], property[1], value).flatMapIterable(AbstractModel::asStatements);


    }

    private String[] splitPrefixedIdentifier(String prefixedKey) {
        String[] property = prefixedKey.split("\\.");
        Assert.isTrue(property.length == 2, "Failed to extract prefix and label from path parameter " + prefixedKey);
        return property;
    }

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
