package io.av360.maverick.graph.feature.applications.api.ext;

import io.av360.maverick.graph.api.controller.AbstractController;
import io.av360.maverick.graph.api.controller.entities.Details;
import io.av360.maverick.graph.model.enums.RdfMimeTypes;
import io.av360.maverick.graph.model.rdf.NamespaceAwareStatement;
import io.av360.maverick.graph.store.rdf.models.TripleBag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping(path = "/api")
@Slf4j(topic = "graph.api.ctrl.details")
@SecurityRequirement(name = "api_key")
public class ScopedDetails extends AbstractController {


    private final Details defaultCtrl;

    public ScopedDetails(Details defaultCtrl) {
        this.defaultCtrl = defaultCtrl;
    }

    @Operation(summary = "Returns all details for a value or link")
    @GetMapping(value = "/sc/{scope}/entities/{id:[\\w|\\d|-|_]+}/{type}/{prefixedValueKey:[\\w|\\d]+\\.[\\w|\\d]+}/details",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> getDetails(
            @PathVariable @Parameter(name = "entity identifier") String id,
            @PathVariable(required = true, value = "values") @Parameter(name = "property type") Details.PropertyType type,
            @PathVariable String prefixedValueKey,
            @RequestParam(required = false) boolean hash
    ) {
        return this.defaultCtrl.getDetails(id, type, prefixedValueKey, hash);
    }

    @Operation(summary = "Purge all details for a value")
    @DeleteMapping(value = "/sc/{scope}/entities/{id:[\\w|\\d|-|_]+}/values/{prefixedValueKey:[\\w|\\d]+\\.[\\w|\\d]+}/details",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> purgeDetails(
            @PathVariable @Parameter(name = "entity identifier") String id,
            @PathVariable String prefixedValueKey
    ) {
        return this.defaultCtrl.purgeDetails(id, prefixedValueKey);
    }

    @Operation(summary = "Delete a specific detail for a value")
    @DeleteMapping(value = "/sc/{scope}/entities/{id:[\\w|\\d|-|_]+}/values/{prefixedValueKey:[\\w|\\d]+\\.[\\w|\\d]+}/details/{prefixedDetailKey:[\\w|\\d]+\\.[\\w|\\d]+}",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> deleteDetail(
            @PathVariable @Parameter(name = "entity identifier") String id,
            @PathVariable String prefixedValueKey,
            @PathVariable String prefixedDetailKey,
            @RequestParam(required = false) boolean multiple,
            @RequestParam(required = false) String hash
    ) {
        return this.defaultCtrl.deleteDetail(id, prefixedValueKey, prefixedDetailKey, multiple, hash);
    }

    @Operation(summary = "Creates a statement about a statement use the post body as value.")
    @PostMapping(value = "/sc/{scope}/entities/{id:[\\w|\\d|-|_]+}/{type}/{prefixedValueKey:[\\w|\\d]+\\.[\\w|\\d]+}/details/{prefixedDetailKey:[\\w|\\d]+\\.[\\w|\\d]+}",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> createDetail(
            @PathVariable @Parameter(name = "entity identifier") String id,
            @PathVariable(required = true, value = "values") @Parameter(name = "property type") Details.PropertyType type,
            @PathVariable String prefixedValueKey,
            @PathVariable String prefixedDetailKey
    ) {
        return this.defaultCtrl.createDetail(id, type, prefixedValueKey, prefixedDetailKey);
    }



    @Operation(summary = "Creates a statement about a statement use the post body as value.")
    @PostMapping(value = "/sc/{scope}/entities/{id:[\\w|\\d|-|_]+}/{type}/{prefixedValueKey:[\\w|\\d]+\\.[\\w|\\d]+}/details",
            consumes = RdfMimeTypes.TURTLESTAR_VALUE,
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> createLinkDetail(
            @PathVariable @Parameter(name = "entity identifier") String id,
            @PathVariable(required = true, value = "values") @Parameter(name = "property type") Details.PropertyType type,
            @PathVariable String prefixedValueKey,
            @RequestBody TripleBag request
    ) {
        return this.defaultCtrl.createLinkDetail(id, type, prefixedValueKey, request);
    }


}

