package io.av360.maverick.graph.api.controller.entities;

import io.av360.maverick.graph.api.controller.AbstractController;
import io.av360.maverick.graph.model.enums.RdfMimeTypes;
import io.av360.maverick.graph.model.rdf.NamespaceAwareStatement;
import io.av360.maverick.graph.services.EntityServices;
import io.av360.maverick.graph.services.ValueServices;
import io.av360.maverick.graph.store.rdf.models.TripleBag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping(path = "/api/entities")
//@Api(tags = "Values")
@Slf4j(topic = "graph.api.entities")
@SecurityRequirement(name = "api_key")
public class Details extends AbstractController {

    private enum PropertyType {
        VALUES,
        LINKS;

        @Override
        public String toString() {
            return super.name().toLowerCase();
        }
    }

    protected final ValueServices values;

    protected final EntityServices entities;

    public Details(ValueServices values, EntityServices entities) {
        this.values = values;
        this.entities = entities;
    }

    @Operation(summary = "Returns all details for a value or link")
    @GetMapping(value = "/{id:[\\w|\\d|-|_]+}/{type}/{prefixedValueKey:[\\w|\\d]+\\.[\\w|\\d]+}/details",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> getDetails(
            @PathVariable @Parameter(name = "entity identifier") String id,
            @PathVariable(required = true, value = "values") @Parameter(name = "property type") PropertyType type,
            @PathVariable String prefixedValueKey,
            @RequestParam(required = false) boolean hash
    ) {
        return Flux.error(new NotImplementedException("Method has not been implemented yet."));
    }

    @Operation(summary = "Purge all details for a value")
    @DeleteMapping(value = "/{id:[\\w|\\d|-|_]+}/values/{prefixedValueKey:[\\w|\\d]+\\.[\\w|\\d]+}/details",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> purgeDetails(
            @PathVariable @Parameter(name = "entity identifier") String id,
            @PathVariable String prefixedValueKey
    ) {
        return Flux.error(new NotImplementedException("Method has not been implemented yet."));
    }

    @Operation(summary = "Delete a specific detail for a value")
    @DeleteMapping(value = "/{id:[\\w|\\d|-|_]+}/values/{prefixedValueKey:[\\w|\\d]+\\.[\\w|\\d]+}/details/{prefixedDetailKey:[\\w|\\d]+\\.[\\w|\\d]+}",
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
        return Flux.error(new NotImplementedException("Method has not been implemented yet."));
    }

    @Operation(summary = "Creates a statement about a statement use the post body as value.")
    @PostMapping(value = "/{id:[\\w|\\d|-|_]+}/{type}/{prefixedValueKey:[\\w|\\d]+\\.[\\w|\\d]+}/details/{prefixedDetailKey:[\\w|\\d]+\\.[\\w|\\d]+}",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> createDetail(
            @PathVariable @Parameter(name = "entity identifier") String id,
            @PathVariable(required = true, value = "values") @Parameter(name = "property type") PropertyType type,
            @PathVariable String prefixedValueKey,
            @PathVariable String prefixedDetailKey
    ) {
        return Flux.error(new NotImplementedException("Method has not been implemented yet."));
    }



    @Operation(summary = "Creates a statement about a statement use the post body as value.")
    @PostMapping(value = "/{id:[\\w|\\d|-|_]+}/{type}/{prefixedValueKey:[\\w|\\d]+\\.[\\w|\\d]+}/details",
            consumes = RdfMimeTypes.TURTLESTAR_VALUE,
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> createLinkDetail(
            @PathVariable @Parameter(name = "entity identifier") String id,
            @PathVariable(required = true, value = "values") @Parameter(name = "property type") PropertyType type,
            @PathVariable String prefixedValueKey,
            @RequestBody TripleBag request
    ) {
        return Flux.error(new NotImplementedException("Method has not been implemented yet."));
    }


}

