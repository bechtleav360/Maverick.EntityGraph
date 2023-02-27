package io.av360.maverick.graph.api.controller.entities;

import io.av360.maverick.graph.api.controller.AbstractController;
import io.av360.maverick.graph.model.enums.RdfMimeTypes;
import io.av360.maverick.graph.model.rdf.NamespaceAwareStatement;
import io.av360.maverick.graph.services.EntityServices;
import io.av360.maverick.graph.services.ValueServices;
import io.av360.maverick.graph.store.rdf.models.TripleModel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping(path = "/api/entities")
//@Api(tags = "Values")
@Slf4j(topic = "graph.api.entities")
@SecurityRequirement(name = "api_key")
public class Links extends AbstractController {


    protected final ValueServices values;

    protected final EntityServices entities;

    public Links(ValueServices values, EntityServices entities) {
        this.values = values;
        this.entities = entities;
    }

    @Operation(summary = "Returns all links of an entity.")
    @GetMapping(value = "/{id:[\\w|\\d|-|_]+}/links",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> getLinks(@PathVariable String id) {
        return Flux.error(new NotImplementedException("Method has not been implemented yet."));
    }
    @Operation(summary = "Returns all links of the given type.")
    @GetMapping(value = "/{id:[\\w|\\d|-|_]+}/links/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> getLinksByType(@PathVariable String id, @PathVariable String prefixedKey) {
        return Flux.error(new NotImplementedException("Method has not been implemented yet."));
    }

    @Operation(summary = "Create edge to existing entity identified by target id.")
    @PutMapping(value = "/{id:[\\w|\\d|-|_]+}/links/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}/{target:[\\w|\\d|-|_]+}",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> createLink(@PathVariable String id, @PathVariable String prefixedKey, @PathVariable String target) {
        return Flux.error(new NotImplementedException("Method has not been implemented yet."));
    }


    @Operation(summary = "Create edge to existing entity identified by target id.")
    @DeleteMapping(value = "/{id:[\\w|\\d|-|_]+}/links/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}/{target:[\\w|\\d|-|_]+}",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> deleteLink(@PathVariable String id, @PathVariable String prefixedKey, @PathVariable String target) {
        return Flux.error(new NotImplementedException("Method has not been implemented yet."));
    }


    @Operation(summary = "Returns all links of the given type.", hidden = true)
    @PutMapping(value = "/{id:[\\w|\\d|-|_]+}/links/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> batchCreateLinks(@PathVariable String id, @PathVariable String prefixedKey) {
        return Flux.error(new NotImplementedException("Method has not been implemented yet."));
    }
}

