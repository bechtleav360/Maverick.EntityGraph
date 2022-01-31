package com.bechtle.eagl.graph.api;

import com.bechtle.eagl.graph.model.Triples;
import com.bechtle.eagl.graph.services.Graph;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(path = "/api/rs",
        consumes = {
            "text/xml",
            "text/nquads",
            "text/turtle",
            "text/n3",
            "text/rdf+n3",
            "application/x-turtle",
            "text/x-turtlestar",
            "application/n-triples",
            "application/n-quads",
            "application/ld+json",
            "application/rdf+xml",
            "application/rdf+json",
            "application/xml",
            "application/trix",
            "application/trig",
            "application/x-trig",
            "application/x-trigstar",
            "application/x-turtlestar",
            "application/x-binary-rdf",
            "application/x-ld+ndjson"
        }
)
@Api( tags = "Entities")
public class Entities {

    protected final ObjectMapper objectMapper;
    protected final Graph graphService;

    public Entities(ObjectMapper objectMapper, Graph graphService) {
        this.objectMapper = objectMapper;
        this.graphService = graphService;
    }

    @ApiOperation(value = "Read entity", tags = {"v1", "entity"})
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    Mono<Triples> read(@PathVariable String id) {
        return graphService.get(id);
    }

    @ApiOperation(value = "Create entity", tags = {"v1", "entity"})
    @PostMapping(value = "/")
    @ResponseStatus(HttpStatus.CREATED)
    Mono<ResponseEntity<Void>>  createEntity(@RequestBody Triples request) {
        return graphService.create(request)
                .map(id -> ResponseEntity.ok().build());
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

    @ApiOperation(value = "Create entity", tags = {"v1", "entity"})
    @PostMapping(value = "/")
    @ResponseStatus(HttpStatus.CREATED)
    Mono<ServerResponse>  createEntity(ServerRequest request);

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

    @ApiOperation(value = "Create value or relation", tags = {"v1", "entity"})
    @PostMapping("/{id}/{prefixedKey}")
    @ResponseStatus(HttpStatus.CREATED)
    Mono<ServerResponse> createValue(@PathVariable String id, @PathVariable String prefixedKey);

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
