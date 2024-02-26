package org.av360.maverick.graph.feature.applications.controller.ext;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.api.controller.*;
import org.av360.maverick.graph.api.controller.dto.Responses;
import org.av360.maverick.graph.api.controller.entities.EntitiesController;
import org.av360.maverick.graph.feature.applications.services.delegates.DelegatingIdentifierServices;
import org.av360.maverick.graph.model.enums.PropertyType;
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
public class ScopedApiOperations extends AbstractController {


    private final DetailsAPI detailsCtrl;

    private final EntitiesAPI entitiesCtrl;

    private final ValuesAPI valuesCtrl;

    private final RelationsAPI linksCtrl;

    private final ContentApi contentCtrl;

    public ScopedApiOperations(DetailsAPI defaultCtrl, EntitiesAPI entitiesCtrl, ValuesAPI valuesCtrl, RelationsAPI linksCtrl, @Nullable ContentApi contentCtrl) {
        this.detailsCtrl = defaultCtrl;
        this.entitiesCtrl = entitiesCtrl;
        this.valuesCtrl = valuesCtrl;
        this.linksCtrl = linksCtrl;
        this.contentCtrl = contentCtrl;
    }

    @GetMapping(value = "/api/s/{label}/entities/{id:[\\w|\\d|\\-|\\_]+}/{type}/{prefixedValueKey:[\\w|\\d]+\\.[\\w|\\d]+}/details",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<Responses.Detail> getDetails(
            @PathVariable @Parameter(name = "entity identifier") String key,
            @PathVariable PropertyType type,
            @PathVariable String prefixedProperty,
            @RequestParam String valueIdentifier
    ) {
        return this.detailsCtrl.getDetails(key, type, prefixedProperty, valueIdentifier);
    }


    @GetMapping(value = "/api/s/{label}/entities/{id:[\\w|\\d|\\-|\\_]+}",
            produces = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<AnnotatedStatement> readAsRDF(@PathVariable String label, @PathVariable String id, @RequestParam(required = false) @Nullable String property) {
        /* since we encode the scope (identified by label) also in the id (e.g. urn:pwi:meg:e:{label}:{id}, we add the scope as prefix
         */
        return entitiesCtrl.readAsRDF(id, property);
    }

    @GetMapping(value = "/api/s/{label}/entities/{id:[\\w|\\d|\\-|\\_]+}",
            produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Mono<Responses.EntityResponse> readAsItem(@PathVariable String label, @PathVariable String id, @RequestParam(required = false) @Nullable String property) {
        /* since we encode the scope (identified by label) also in the id (e.g. urn:pwi:meg:e:{label}:{id}, we add the scope as prefix
         */
        return entitiesCtrl.readAsItem(id, property);
    }

    @GetMapping(value = "/api/s/{label}/entities", produces = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<AnnotatedStatement> list(
            @PathVariable String label,
            @RequestParam(value = "limit", defaultValue = "100") Integer limit,
            @RequestParam(value = "offset", defaultValue = "0") Integer offset) {
        return entitiesCtrl.list(limit, offset).doOnSubscribe(sub -> log.trace("Request within scope {}", label));
    }


    @GetMapping(value = "/api/s/{label}/entities/{id:[\\w|\\d|\\-|\\_]+}/values",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<AnnotatedStatement> listValues(@PathVariable String label, @PathVariable String id, @RequestParam(required = false) String prefixedKey) {
        return this.valuesCtrl.list(id, prefixedKey);
    }

    @GetMapping(value = "/api/s/{label}/entities/{id:[\\w|\\d|\\-|\\_]+}/values",
            produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<Responses.ValueObject> listValuesAsJson(@PathVariable String label, @PathVariable String id, @RequestParam(required = false) String prefixedKey) {
        return this.valuesCtrl.listAsJson(id, prefixedKey);
    }

    @GetMapping(value = "/api/s/{label}/entities/{id:[\\w|\\d|-|_]+}/links",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<AnnotatedStatement> getLinks(@PathVariable String label, @PathVariable String id) {
        return this.linksCtrl.list(id);
    }

    @GetMapping(value = "/api/s/{label}/entities/{id:[\\w|\\d|\\-|\\_]+}/links/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<AnnotatedStatement> getLinksByType(@PathVariable String label, @PathVariable String id, @PathVariable String prefixedKey) {
        return this.linksCtrl.getLinksByType(id, prefixedKey);
    }

    @GetMapping(value = "/content/s/{label}/{id:[\\w|\\d|\\-|\\_]+}")

    @ResponseStatus(HttpStatus.OK)
    Mono<ResponseEntity<Flux<DataBuffer>>> getContent(@PathVariable String label, @PathVariable String id) {
        if (Objects.isNull(this.contentCtrl)) {
            return Mono.just(ResponseEntity.noContent().build());
        }
        return this.contentCtrl.download(id);
    }


}

