package cougar.graph.api.controller.entities;

import com.fasterxml.jackson.databind.ObjectMapper;
import cougar.graph.api.controller.AbstractController;
import cougar.graph.model.enums.RdfMimeTypes;
import cougar.graph.model.rdf.GeneratedIdentifier;
import cougar.graph.model.rdf.NamespaceAwareStatement;
import cougar.graph.services.services.EntityServices;
import cougar.graph.services.services.QueryServices;
import cougar.graph.store.rdf.models.AbstractModel;
import cougar.graph.store.rdf.models.Incoming;
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
@Slf4j(topic = "graph.api.entities")
public class Entities extends AbstractController {

    protected final ObjectMapper objectMapper;
    protected final EntityServices entityServices;
    protected final QueryServices queryServices;

    public Entities(ObjectMapper objectMapper, EntityServices graphService, QueryServices queryServices) {
        this.objectMapper = objectMapper;
        this.entityServices = graphService;
        this.queryServices = queryServices;
    }

    @ApiOperation(value = "Read entity")
    @GetMapping(value = "/{id:[\\w|\\d|-|_]+}",
                produces = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> read(@PathVariable String id) {
        Assert.isTrue(id.length() == GeneratedIdentifier.LENGTH, "Incorrect length for identifier.");

        return super.getAuthentication()
                .flatMap(authentication -> entityServices.readEntity(id, authentication))
                .flatMapIterable(AbstractModel::asStatements)
                .doOnSubscribe(s -> {
                    if(log.isDebugEnabled()) log.debug("Request to read entity with id: {}", id);
                });
    }


    @ApiOperation(value = "List entities")
    @GetMapping(value = "", produces = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> list() {

        return super.getAuthentication()
                .flatMapMany(queryServices::listEntities)
                .flatMapIterable(AbstractModel::asStatements)
                .doOnSubscribe(s -> {
                    if(log.isDebugEnabled()) log.debug("Request to list entities");
                });
    }

    @ApiOperation(value = "Create entity")
    @PostMapping(value = "",
            consumes = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE},
            produces = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE})
    @ResponseStatus(HttpStatus.ACCEPTED)
    Flux<NamespaceAwareStatement> createEntity(@RequestBody Incoming request) {
        Assert.isTrue(request.getModel().size() > 0, "No statements in request detected.");

        return super.getAuthentication()
                .flatMap(authentication ->  entityServices.createEntity(request, Map.of(), authentication))
                .flatMapIterable(AbstractModel::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled()) log.debug("Request to create a new Entity");
                    if (log.isTraceEnabled()) log.trace("Payload: \n {}", request.toString());
                });
    }


    @ApiOperation(value = "Create value or relation")
    @PostMapping(value = "/{id:[\\w|\\d|-|_]+}/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.CREATED)
    Flux<NamespaceAwareStatement> createValue(@PathVariable String id, @PathVariable String prefixedKey, @RequestBody String value) {
        Assert.isTrue(!value.matches("(?s).*[\\n\\r].*"), "Newlines in request body are not supported");

        String[] property = splitPrefixedIdentifier(prefixedKey);
        return super.getAuthentication()
                .flatMap(authentication ->  entityServices.setValue(id, property[0], property[1], value, authentication))
                .flatMapIterable(AbstractModel::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled()) log.debug("Request to set property '{}' of entity '{}' to value '{}'", prefixedKey, id, value.length() > 64 ? value.substring(0, 64) : value);
                });

    }

    @ApiOperation(value = "Create value or relation")
    @PostMapping(value = "/{id:[\\w|\\d|-|_]+}/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}",
            consumes = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE},
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.CREATED)
    Flux<NamespaceAwareStatement> createEmbedded(@PathVariable String id, @PathVariable String prefixedKey, @RequestBody Incoming value) {

        String[] property = splitPrefixedIdentifier(prefixedKey);
        return super.getAuthentication()
                .flatMap(authentication ->  entityServices.linkEntityTo(id, property[0], property[1], value, authentication))
                .flatMapIterable(AbstractModel::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled()) log.debug("Request to add embedded entities as property '{}' to entity '{}'", prefixedKey, id);
                });


    }
    //-----------------------------------------------------------------


    @ApiOperation(value = "Delete entity")
    @DeleteMapping(value = "/{id:[\\w|\\d|-|_]+}",
            produces = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> deleteEntity(@PathVariable String id){
        Assert.isTrue(id.length() == GeneratedIdentifier.LENGTH, "Incorrect length for identifier.");

        return super.getAuthentication()
                .flatMap(authentication ->  entityServices.deleteEntity(id, authentication))
                .flatMapIterable(AbstractModel::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled()) log.debug("Delete an Entity");
                    if (log.isTraceEnabled()) log.trace("id: \n {}", id);
                });
    }

    @ApiOperation(value = "Delete value or relation", tags = {"v2", "entity"})
    @DeleteMapping(value = "/{id:[\\w|\\d|-|_]+}/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.CREATED)
    Flux<NamespaceAwareStatement> deleteValue(@PathVariable String id, @PathVariable String prefixedKey, @RequestBody String value) {
        Assert.isTrue(!value.matches("(?s).*[\\n\\r].*"), "Newlines in request body are not supported");

        String[] property = splitPrefixedIdentifier(prefixedKey);
        return super.getAuthentication()
                .flatMap(authentication ->  entityServices.deleteValue(id, property[0], property[1], value, authentication))
                .flatMapIterable(AbstractModel::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled()) log.debug("Deleted property '{}' of entity '{}'", prefixedKey, id);
                });

    }

    @ApiOperation(value = "Update value", tags = {"v3", "entity"})
    @PutMapping(value = "/{id:[\\w|\\d|-|_]+}/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.CREATED)
    Flux<NamespaceAwareStatement> updateValue(@PathVariable String id, @PathVariable String prefixedKey, @RequestBody String value) {
        //TODO: Assert prefixedKey must exist in advance
        String[] property = splitPrefixedIdentifier(prefixedKey);
        return super.getAuthentication()
                .flatMap(authentication -> entityServices.setValue(id, property[0], property[1], value, authentication))
                .flatMapIterable(AbstractModel::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled())
                        log.debug("Set property '{}' of entity '{}' to value '{}'", prefixedKey, id, value.length() > 64 ? value.substring(0, 64) : value);
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
