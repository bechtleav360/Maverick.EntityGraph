package org.av360.maverick.graph.api.controller.entities;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.av360.maverick.graph.api.controller.AbstractController;
import org.av360.maverick.graph.model.api.LinksAPI;
import org.av360.maverick.graph.model.enums.RdfMimeTypes;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.model.rdf.Triples;
import org.av360.maverick.graph.services.EntityServices;
import org.av360.maverick.graph.services.SchemaServices;
import org.av360.maverick.graph.services.ValueServices;
import org.av360.maverick.graph.store.rdf.fragments.TripleModel;
import org.reactivestreams.Subscription;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;

@RestController
@RequestMapping(path = "/api")
//@Api(tags = "Values")
@Slf4j(topic = "graph.ctrl.api.links")
@SecurityRequirement(name = "api_key")
@Tag(name = "Annotations")
public class LinksController extends AbstractController implements LinksAPI {


    protected final ValueServices values;

    protected final EntityServices entities;

    protected final SchemaServices schemaServices;

    public LinksController(ValueServices values, EntityServices entities, SchemaServices schemaServices) {
        this.values = values;
        this.entities = entities;
        this.schemaServices = schemaServices;
    }

    @Override
    @Operation(summary = "Returns all links of an entity.")
    @GetMapping(value = "/entities/{id:[\\w|\\d|\\-|\\_]+}/links",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public Flux<AnnotatedStatement> getLinks(@PathVariable String id) {
        return Flux.error(new NotImplementedException("Method has not been implemented yet."));
    }

    @Override
    @Operation(summary = "Returns all links of the given type.")
    @GetMapping(value = "/entities/{id:[\\w|\\d|\\-|\\_]+}/links/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public Flux<AnnotatedStatement> getLinksByType(@PathVariable String id, @PathVariable String prefixedKey) {
        return super.acquireContext()
                .flatMap(ctx -> this.values.listLinks(id, prefixedKey, ctx))
                .flatMapIterable(TripleModel::asStatements)
                .doOnSubscribe((Subscription s) -> {
                    if (log.isDebugEnabled())
                        log.debug("Request to get all '{}' links for entity '{}'", prefixedKey, id);
                });

    }

    @Override
    @Operation(summary = "Create edge to existing entity identified by target id (within the same dataset).",
            description = """
                    Creates an edge to an existing entity within the same graph (application) identified by the target identifier to link the two entities. An edge represents a relationship between 
                    two nodes in the graph, while the node represents an entity in the graph. When creating an edge to an existing entity, the target identifier
                    is used to identify the specific entity to which the edge will be created. The creation of the edge fails, if no entity with the target identifier exists.      
                    Multiple edges can connect the same two nodes, representing different relationships between the entities they represent. For example, in a social network,
                    two users can be connected by multiple edges representing different types of relationships, such as friendship, family, or work-related connections.
                                        
                    To create a link to an entity within a different application (or externally managed entity), use the values api instead. 
                    """

    )
    @PutMapping(value = "/entities/{source_id:[\\w|\\d|\\-|\\_]+}/links/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}/{target_id:[\\w|\\d|\\-|\\_]+}",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.CREATED)
    public Flux<AnnotatedStatement> createLink(@PathVariable String source_id, @PathVariable String prefixedKey, @PathVariable String target_id, @Nullable @RequestParam(required = false) Boolean replace) {

        return super.acquireContext()
                .flatMap(ctx -> this.values.insertLink(source_id, prefixedKey, target_id, replace, ctx))
                .flatMapIterable(Triples::asStatements)
                .doOnSubscribe((Subscription s) -> {
                    if (log.isDebugEnabled())
                        log.debug("Request to create link '{}' between entity '{}' and entity '{}'", prefixedKey, source_id, target_id);
                });

    }


    @Override
    @Operation(summary = "Deletes edge to existing entity identified by target id.")
    @DeleteMapping(value = "/entities/{source_id:[\\w|\\d|\\-|\\_]+}/links/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}/{target_id:[\\w|\\d|-|_]+}", 
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public Flux<AnnotatedStatement> deleteLink(@PathVariable String source_id, @PathVariable String prefixedKey, @PathVariable String target_id) {
        return super.acquireContext()
                .flatMap(ctx -> this.values.removeLink(source_id, prefixedKey, target_id, ctx))
                .flatMap(Mono::just)
                .flatMapIterable(Triples::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled())
                        log.debug("Request to remove link '{}' between entity '{}' and entity '{}'", prefixedKey, source_id, target_id);
                })
                .doOnComplete(() -> {
                    if (log.isDebugEnabled())
                        log.debug("Request to remove link '{}' between entity '{}' and entity '{}' completed", prefixedKey, source_id, target_id);
                })
                .doOnError(error -> {
                    log.warn("Failed request to remove link '{}' between entity '{}' and entity '{}'", prefixedKey, source_id, target_id);
                });


    }


    @Override
    @Operation(summary = "Returns all links of the given type.", hidden = true)
    @PutMapping(value = "/entities/{id:[\\w|\\d|\\-|\\_]+}/links/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public Flux<AnnotatedStatement> batchCreateLinks(@PathVariable String id, @PathVariable String prefixedKey) {
        return Flux.error(new NotImplementedException("Method has not been implemented yet."));
    }
}

