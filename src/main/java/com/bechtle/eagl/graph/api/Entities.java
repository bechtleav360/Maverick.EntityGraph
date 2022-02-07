package com.bechtle.eagl.graph.api;

import com.bechtle.eagl.graph.model.wrapper.AbstractModelWrapper;
import com.bechtle.eagl.graph.model.wrapper.IncomingStatements;
import com.bechtle.eagl.graph.model.NamespaceAwareStatement;
import com.bechtle.eagl.graph.model.wrapper.Transaction;
import com.bechtle.eagl.graph.model.enums.RdfMimeTypes;
import com.bechtle.eagl.graph.services.EntityServices;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InvalidObjectException;

@RestController
@RequestMapping(path = "/api/rs")
@Api(tags = "Entities")
@Slf4j
public class Entities {

    protected final ObjectMapper objectMapper;
    protected final EntityServices graphService;

    public Entities(ObjectMapper objectMapper, EntityServices graphService) {
        this.objectMapper = objectMapper;
        this.graphService = graphService;
    }

    @ApiOperation(value = "Read entity", tags = {"v1"})
    @GetMapping(value = "/{id:[\\w|\\d|-|_]+}", produces = { RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.NQUADS_VALUE })
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> read(@PathVariable String id) {
        log.trace("(Request) Reading Entity with id: {}", id);
        return graphService.readEntity(id).flatMapIterable(AbstractModelWrapper::asStatements);
    }

    @ApiOperation(value = "Create entity", tags = {"v1"})
    @PostMapping(value = "",
            consumes = { RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE },
            produces = { RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.NQUADS_VALUE })
    @ResponseStatus(HttpStatus.ACCEPTED)
    Flux<NamespaceAwareStatement> createEntity(@RequestBody IncomingStatements request) {
        log.trace("(Request) Create Entity with payload: {}", request.toString());
        try {
            return graphService.createEntity(request).flatMapIterable(AbstractModelWrapper::asStatements);
        } catch (InvalidObjectException e) {
            log.warn("Invalid request while creating entity. ", e);
            return Flux.error(new HttpClientErrorException(HttpStatus.BAD_REQUEST));
        } catch (IOException e) {
            log.error("Exception while creating entity. ", e);
            return Flux.error(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }



    @ApiOperation(value = "Create value or relation", tags = {"v1"})
    @PostMapping(value = "/{id:[\\w|\\d|-|_]+}/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = { RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.CREATED)
    Flux<NamespaceAwareStatement> createValue(@PathVariable String id, @PathVariable String prefixedKey, @RequestBody String value) {
        log.trace("(Request) Set value '{}' for property '{}' of entity '{}'", value.toString(), prefixedKey, id);
        if(value.matches(".*\n.*")) return Flux.error(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Newlines in request are not supported"));

        String[] property = prefixedKey.split("\\.");
        Assert.isTrue(property.length == 2, "Failed to extract prefix and name from path parameter "+prefixedKey);

        return graphService.setValue(id, property[0], property[1], value).flatMapIterable(AbstractModelWrapper::asStatements);

       /* } catch (InvalidObjectException e) {
            log.warn("Invalid request while saving value. ", e);
            return Flux.error(new HttpClientErrorException(HttpStatus.BAD_REQUEST));
        } catch (IOException e) {
            log.error("Exception while saving value. ", e);
            return Flux.error(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));
        }*/
    }

    @ApiOperation(value = "Create value or relation", tags = {"v1"})
    @PostMapping(value = "/{id:[\\w|\\d|-|_]+}/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}",
            consumes = { RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE },
            produces = { RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.CREATED)
    Flux<NamespaceAwareStatement> createEmbedded(@PathVariable String id, @PathVariable String prefixedKey, @RequestBody IncomingStatements value)  {
        log.trace("(Request) Add embedded entity to property '{}' of entity '{}'", prefixedKey, id);
        String[] property = prefixedKey.split("\\.");
        Assert.isTrue(property.length == 2, "Failed to extract prefix and name from path parameter "+prefixedKey);
        try {
            return graphService.link(id, property[0], property[1], value).flatMapIterable(AbstractModelWrapper::asStatements);
        } catch (IOException e) {
            log.error("Exception while linking embedded. ", e);
            return Flux.error(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));
        }


       /* } catch (InvalidObjectException e) {
            log.warn("Invalid request while saving value. ", e);
            return Flux.error(new HttpClientErrorException(HttpStatus.BAD_REQUEST));
        } catch (IOException e) {

        }*/
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
