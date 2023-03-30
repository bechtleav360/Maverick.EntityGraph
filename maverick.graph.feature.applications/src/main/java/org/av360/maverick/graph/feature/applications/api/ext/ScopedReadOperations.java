package org.av360.maverick.graph.feature.applications.api.ext;

import org.av360.maverick.graph.model.enums.RdfMimeTypes;
import org.av360.maverick.graph.model.rdf.NamespaceAwareStatement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.api.controller.AbstractController;
import org.av360.maverick.graph.api.controller.entities.Details;
import org.av360.maverick.graph.api.controller.entities.Entities;
import org.av360.maverick.graph.api.controller.entities.Links;
import org.av360.maverick.graph.api.controller.entities.Values;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import javax.annotation.Nullable;

@RestController
@RequestMapping(path = "/api")
@Slf4j(topic = "graph.api.ctrl.details")
@SecurityRequirement(name = "api_key")
@Tag(name = "Scoped Read Operations")
public class ScopedReadOperations extends AbstractController {


    private final Details detailsCtrl;

    private final Entities entitiesCtrl;

    private final Values valuesCtrl;

    private final Links linksCtrl;

    public ScopedReadOperations(Details defaultCtrl, Entities entitiesCtrl, Values valuesCtrl, Links linksCtrl) {
        this.detailsCtrl = defaultCtrl;
        this.entitiesCtrl = entitiesCtrl;
        this.valuesCtrl = valuesCtrl;
        this.linksCtrl = linksCtrl;
    }

    @Operation(summary = "Returns all details for a value or link")
    @GetMapping(value = "/app/{label}/entities/{id:[\\w|\\d|-|_]+}/{type}/{prefixedValueKey:[\\w|\\d]+\\.[\\w|\\d]+}/details",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> getDetails(
            @PathVariable @Parameter(name = "entity identifier") String id,
            @PathVariable(required = true, value = "values") @Parameter(name = "property type") Details.PropertyType type,
            @PathVariable String prefixedValueKey,
            @RequestParam(required = false) boolean hash
    ) {
        return this.detailsCtrl.getDetails(id, type, prefixedValueKey, hash);
    }


    @GetMapping(value = "/app/{label}/entities/{id:[\\w|\\d|-|_]+}",
            produces = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> read(@PathVariable String scope, @PathVariable String id, @RequestParam(required = false) @Nullable String property) {
        return entitiesCtrl.read(id, property);
    }

    @GetMapping(value = "/app/{label}/entities", produces = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> list(
            @PathVariable String scope,
            @RequestParam(value = "limit", defaultValue = "100") Integer limit,
            @RequestParam(value = "offset", defaultValue = "0") Integer offset) {
        return entitiesCtrl.list(limit, offset).doOnSubscribe(sub -> log.trace("Request within scope {}", scope));
    }


    @Operation(summary = "Returns a list of value properties of the selected entity.  ")
    @GetMapping(value = "/app/{label}/entities/{id:[\\w|\\d|-|_]+}/values",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> listEntityValues(@PathVariable String id) {
        return this.valuesCtrl.listEntityValues(id);
    }

    @Operation(summary = "Returns all links of an entity.")
    @GetMapping(value = "/app/{label}/entities/{id:[\\w|\\d|-|_]+}/links",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> getLinks(@PathVariable String id) {
        return this.linksCtrl.getLinks(id);
    }

    @Operation(summary = "Returns all links of the given type.")
    @GetMapping(value = "/app/{scope}/entities/{id:[\\w|\\d|-|_]+}/links/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> getLinksByType(@PathVariable String id, @PathVariable String prefixedKey) {
        return this.linksCtrl.getLinksByType(id, prefixedKey);
    }


}

