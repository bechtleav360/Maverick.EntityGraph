package io.av360.maverick.graph.feature.applications.api.ext;

import io.av360.maverick.graph.api.controller.AbstractController;
import io.av360.maverick.graph.api.controller.entities.Links;
import io.av360.maverick.graph.model.enums.RdfMimeTypes;
import io.av360.maverick.graph.model.rdf.NamespaceAwareStatement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping(path = "/api")
//@Api(tags = "Values")
@Slf4j(topic = "graph.ctrl.api.links")
@SecurityRequirement(name = "api_key")
public class ScopedLinks extends AbstractController {

    private final Links defaultCtrl;

    public ScopedLinks(Links defaultCtrl) {
        this.defaultCtrl = defaultCtrl;
    }

    @Operation(summary = "Returns all links of an entity.")
    @GetMapping(value = "/sc/{scope}/entities/{id:[\\w|\\d|-|_]+}/links",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> getLinks(@PathVariable String id) {
        return this.defaultCtrl.getLinks(id);
    }
    @Operation(summary = "Returns all links of the given type.")
    @GetMapping(value = "/sc/{scope}/entities/{id:[\\w|\\d|-|_]+}/links/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> getLinksByType(@PathVariable String id, @PathVariable String prefixedKey) {
        return this.defaultCtrl.getLinksByType(id, prefixedKey);
    }

    @Operation(summary = "Create edge to existing entity identified by target id.")
    @PutMapping(value = "/sc/{scope}/entities/{id:[\\w|\\d|-|_]+}/links/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}/{target:[\\w|\\d|-|_]+}",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> createLink(@PathVariable String id, @PathVariable String prefixedKey, @PathVariable String target) {
        return this.defaultCtrl.createLink(id, prefixedKey, target);
    }


    @Operation(summary = "Create edge to existing entity identified by target id.")
    @DeleteMapping(value = "/sc/{scope}/entities/{id:[\\w|\\d|-|_]+}/links/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}/{target:[\\w|\\d|-|_]+}",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> deleteLink(@PathVariable String id, @PathVariable String prefixedKey, @PathVariable String target) {
        return this.defaultCtrl.deleteLink(id, prefixedKey, target);
    }


    @Operation(summary = "Returns all links of the given type.", hidden = true)
    @PutMapping(value = "/sc/{scope}/entities/{id:[\\w|\\d|-|_]+}/links/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> batchCreateLinks(@PathVariable String id, @PathVariable String prefixedKey) {
        return this.defaultCtrl.batchCreateLinks(id, prefixedKey);
    }
}

