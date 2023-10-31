package org.av360.maverick.graph.feature.applications.controller.ext;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.api.controller.AbstractController;
import org.av360.maverick.graph.api.controller.entities.DetailsController;
import org.av360.maverick.graph.api.controller.entities.EntitiesController;
import org.av360.maverick.graph.feature.applications.services.delegates.DelegatingIdentifierServices;
import org.av360.maverick.graph.model.api.*;
import org.av360.maverick.graph.model.enums.RdfMimeTypes;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Supports links where the scope is encoded in the path in the form
 * <pre> host.com/api/s/{scope}/entities/{id}</pre>
 *
 * All requests are delegated to the default operator, the scope is extracted by the filter (the correct IDs are then
 * extracted by the delegating Identifier service)
 *
 * @see EntitiesController
 * @see org.av360.maverick.graph.feature.applications.config.RequestedApplicationFilter
 * @see DelegatingIdentifierServices
 */
@RestController
@RequestMapping(path = "")
@Slf4j(topic = "graph.api.ctrl.details")
@SecurityRequirement(name = "api_key")
@Tag(name = "Scoped Read Operations")
public class ScopedApiOperations extends AbstractController {


    private final DetailsAPI detailsCtrl;

    private final EntitiesAPI entitiesCtrl;

    private final ValuesAPI valuesCtrl;

    private final LinksAPI linksCtrl;

    private final ContentApi contentCtrl;

    public ScopedApiOperations(DetailsAPI defaultCtrl, EntitiesAPI entitiesCtrl, ValuesAPI valuesCtrl, LinksAPI linksCtrl, @Nullable ContentApi contentCtrl) {
        this.detailsCtrl = defaultCtrl;
        this.entitiesCtrl = entitiesCtrl;
        this.valuesCtrl = valuesCtrl;
        this.linksCtrl = linksCtrl;
        this.contentCtrl = contentCtrl;
    }

    @Operation(summary = "Returns all details for a value or link")
    @GetMapping(value = "/api/s/{label}/entities/{id:[\\w|\\d|\\-|\\_]+}/{type}/{prefixedValueKey:[\\w|\\d]+\\.[\\w|\\d]+}/details",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<AnnotatedStatement> getDetails(
            @PathVariable String label,
            @PathVariable @Parameter(name = "entity identifier") String id,
            @PathVariable(required = true, value = "values") @Parameter(name = "property type") DetailsController.PropertyType type,
            @PathVariable String prefixedValueKey,
            @RequestParam(required = false) boolean hash
    ) {
        return this.detailsCtrl.getDetails(id, type, prefixedValueKey, hash);
    }


    @GetMapping(value = "/api/s/{label}/entities/{id:[\\w|\\d|\\-|\\_]+}",
            produces = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<AnnotatedStatement> read(@PathVariable String label, @PathVariable String id, @RequestParam(required = false) @Nullable String property) {
        /* since we encode the scope (identified by label) also in the id (e.g. urn:pwi:meg:e:{label}:{id}, we add the scope as prefix
         */
        return entitiesCtrl.read(id, property);
    }

    @GetMapping(value = "/api/s/{label}/entities", produces = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<AnnotatedStatement> list(
            @PathVariable String label,
            @RequestParam(value = "limit", defaultValue = "100") Integer limit,
            @RequestParam(value = "offset", defaultValue = "0") Integer offset) {
        return entitiesCtrl.list(limit, offset).doOnSubscribe(sub -> log.trace("Request within scope {}", label));
    }


    @Operation(summary = "Returns a list of value configuration of the selected entity.  ")
    @GetMapping(value = "/api/s/{label}/entities/{id:[\\w|\\d|\\-|\\_]+}/values",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<AnnotatedStatement> listEntityValues(@PathVariable String label, @PathVariable String id, @RequestParam(required = false) String prefixedKey) {
        return this.valuesCtrl.list(id, prefixedKey);
    }

    @Operation(summary = "Returns all links of an entity.")
    @GetMapping(value = "/api/s/{label}/entities/{id:[\\w|\\d|-|_]+}/links",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<AnnotatedStatement> getLinks(@PathVariable String label, @PathVariable String id) {
        return this.linksCtrl.getLinks(id);
    }

    @Operation(summary = "Returns all links of the given type.")
    @GetMapping(value = "/api/s/{label}/entities/{id:[\\w|\\d|\\-|\\_]+}/links/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<AnnotatedStatement> getLinksByType(@PathVariable String label, @PathVariable String id, @PathVariable String prefixedKey) {
        return this.linksCtrl.getLinksByType(id, prefixedKey);
    }

    @Operation(summary = "Returns object content.")
    @GetMapping(value = "/content/s/{label}/{id:[\\w|\\d|\\-|\\_]+}")

    @ResponseStatus(HttpStatus.OK)
    Mono<ResponseEntity<Flux<DataBuffer>>> getContent(@PathVariable String label, @PathVariable String id) {
        if (Objects.isNull(this.contentCtrl)) {
            return Mono.just(ResponseEntity.noContent().build());
        }
        return this.contentCtrl.download(id);
    }


}

