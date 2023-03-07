package io.av360.maverick.graph.api.controller.entities;

import io.av360.maverick.graph.api.controller.AbstractController;
import io.av360.maverick.graph.model.enums.RdfMimeTypes;
import io.av360.maverick.graph.model.rdf.NamespaceAwareStatement;
import io.av360.maverick.graph.services.EntityServices;
import io.av360.maverick.graph.services.SchemaServices;
import io.av360.maverick.graph.services.ValueServices;
import io.av360.maverick.graph.store.rdf.models.TripleModel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.reactivestreams.Subscription;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(path = "/api/entities")
//@Api(tags = "Values")
@Slf4j(topic = "graph.ctrl.api.links")
@SecurityRequirement(name = "api_key")
public class Links extends AbstractController {


    protected final ValueServices values;

    protected final EntityServices entities;

    protected final SchemaServices schemaServices;

    public Links(ValueServices values, EntityServices entities, SchemaServices schemaServices) {
        this.values = values;
        this.entities = entities;
        this.schemaServices = schemaServices;
    }

    @Operation(summary = "Returns all links of an entity.")
    @GetMapping(value = "/{id:[\\w|\\d|-|_]+}/links",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public Flux<NamespaceAwareStatement> getLinks(@PathVariable String id) {
        return Flux.error(new NotImplementedException("Method has not been implemented yet."));
    }

    @Operation(summary = "Returns all links of the given type.")
    @GetMapping(value = "/{id:[\\w|\\d|-|_]+}/links/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public Flux<NamespaceAwareStatement> getLinksByType(@PathVariable String id, @PathVariable String prefixedKey) {
        return Flux.error(new NotImplementedException("Method has not been implemented yet."));
    }

    @Operation(summary = "Create edge to existing entity identified by target id (within the same application).",
            description = """
                    Creates an edge to an existing entity within the same graph (application) identified by the target identifier to link the two entities. An edge represents a relationship between 
                    two nodes in the graph, while the node represents an entity in the graph. When creating an edge to an existing entity, the target identifier
                    is used to identify the specific entity to which the edge will be created. The creation of the edge fails, if no entity with the target identifier exists.      
                    Multiple edges can connect the same two nodes, representing different relationships between the entities they represent. For example, in a social network,
                    two users can be connected by multiple edges representing different types of relationships, such as friendship, family, or work-related connections.
                                        
                    To create a link to an entity within a different application (or externally managed entity), use the values api instead. 
                    """

    )
    @PutMapping(value = "/{source_id:[\\w|\\d|-|_]+}/links/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}/{target_id:[\\w|\\d|-|_]+}",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.CREATED)
    public Flux<NamespaceAwareStatement> createLink(@PathVariable String source_id, @PathVariable String prefixedKey, @PathVariable String target_id) {

        return super.getAuthentication()
                .flatMap(authentication -> this.values.insertLink(source_id, prefixedKey, target_id, authentication))
                .flatMapIterable(TripleModel::asStatements)
                .doOnSubscribe((Subscription s) -> {
                    if (log.isDebugEnabled())
                        log.debug("Request to create link '{}' between entity '{}' and entity '{}'", prefixedKey, source_id, target_id);
                });

    }


    @Operation(summary = "Deletes edge to existing entity identified by target id.")
    @DeleteMapping(value = "/{source_id:[\\w|\\d|-|_]+}/links/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}/{target_id:[\\w|\\d|-|_]+}")
    @ResponseStatus(HttpStatus.OK)
    public Flux<NamespaceAwareStatement> deleteLink(@PathVariable String source_id, @PathVariable String prefixedKey, @PathVariable String target_id) {
        return super.getAuthentication()
                .flatMap(authentication -> this.values.removeLink(source_id, prefixedKey, target_id, authentication))
                .flatMap(trx -> {
                    return Mono.just(trx);
                })
                .flatMapIterable(TripleModel::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled())
                        log.debug("Request to remove link '{}' between entity '{}' and entity '{}'", prefixedKey, source_id, target_id);
                })
                .doOnComplete(() -> {
                    if (log.isDebugEnabled())
                        log.debug("Request to remove link '{}' between entity '{}' and entity '{}' completed", prefixedKey, source_id, target_id);
                });


    }


    @Operation(summary = "Returns all links of the given type.", hidden = true)
    @PutMapping(value = "/{id:[\\w|\\d|-|_]+}/links/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public Flux<NamespaceAwareStatement> batchCreateLinks(@PathVariable String id, @PathVariable String prefixedKey) {
        return Flux.error(new NotImplementedException("Method has not been implemented yet."));
    }
}

