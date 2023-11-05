package org.av360.maverick.graph.model.api;

import io.swagger.v3.oas.annotations.Operation;
import org.av360.maverick.graph.model.enums.RdfMimeTypes;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import javax.annotation.Nullable;

public interface RelationsAPI {
    @Operation(summary = "Returns all links of an entity.")
    @GetMapping(value = "/entities/{id:[\\w|\\d|\\-|\\_]+}/links",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<AnnotatedStatement> getLinks(@PathVariable String id);

    @Operation(summary = "Returns all links of the given type.")
    @GetMapping(value = "/entities/{id:[\\w|\\d|\\-|\\_]+}/links/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<AnnotatedStatement> getLinksByType(@PathVariable String id, @PathVariable String prefixedKey);

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
    @PutMapping(value = "/entities/{source_id:[\\w|\\d|\\-|\\_]+}/links/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}/{target_id:[\\w|\\d|\\-|\\_]+}",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.CREATED)
    Flux<AnnotatedStatement> createLink(@PathVariable String source_id, @PathVariable String prefixedKey, @PathVariable String target_id, @Nullable @RequestParam(required = false) Boolean replace);

    @Operation(summary = "Deletes edge to existing entity identified by target id.")
    @DeleteMapping(value = "/entities/{source_id:[\\w|\\d|\\-|\\_]+}/links/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}/{target_id:[\\w|\\d|-|_]+}",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<AnnotatedStatement> deleteLink(@PathVariable String source_id, @PathVariable String prefixedKey, @PathVariable String target_id);

    @Operation(summary = "Returns all links of the given type.", hidden = true)
    @PutMapping(value = "/entities/{id:[\\w|\\d|\\-|\\_]+}/links/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<AnnotatedStatement> batchCreateLinks(@PathVariable String id, @PathVariable String prefixedKey);
}
