package org.av360.maverick.graph.model.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.av360.maverick.graph.model.enums.RdfMimeTypes;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.model.rdf.Triples;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

public interface DetailsAPI {
    enum PropertyType {
        VALUES,
        LINKS;

        @Override
        public String toString() {
            return super.name().toLowerCase();
        }
    }

    @Operation(summary = "Returns all details for a value or link")
    @GetMapping(value = "/entities/{id:[\\w|\\d|\\-|\\_]+}/{type}/{prefixedValueKey:[\\w|\\d]+\\.[\\w|\\d]+}/details",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<AnnotatedStatement> getDetails(
            @PathVariable @Parameter(name = "entity identifier") String id,
            @PathVariable(required = true, value = "values") @Parameter(name = "property type") DetailsAPI.PropertyType type,
            @PathVariable String prefixedValueKey,
            @RequestParam(required = false) boolean hash
    );

    @Operation(summary = "Purge all details for a value")
    @DeleteMapping(value = "/entities/{id:[\\w|\\d|\\-|\\_]+}/values/{prefixedValueKey:[\\w|\\d]+\\.[\\w|\\d]+}/details",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<AnnotatedStatement> purgeDetails(
            @PathVariable @Parameter(name = "entity identifier") String id,
            @PathVariable String prefixedValueKey
    );

    @Operation(summary = "Delete a specific detail for a value")
    @DeleteMapping(value = "/entities/{id:[\\w|\\d|\\-|\\_]+}/values/{prefixedValueKey:[\\w|\\d]+\\.[\\w|\\d]+}/details/{prefixedDetailKey:[\\w|\\d]+\\.[\\w|\\d]+}",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<AnnotatedStatement> deleteDetail(
            @PathVariable @Parameter(name = "entity identifier") String id,
            @PathVariable String prefixedValueKey,
            @PathVariable String prefixedDetailKey,
            @RequestParam(required = false) boolean multiple,
            @RequestParam(required = false) String hash
    );


    @Operation(summary = "Creates a statement about a statement use the post body as value.")
    @PostMapping(value = "/entities/{id:[\\w|\\d|\\-|\\_]+}/{type}/{prefixedValueKey:[\\w|\\d]+\\.[\\w|\\d]+}/details/{prefixedDetailKey:[\\w|\\d]+\\.[\\w|\\d]+}",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<AnnotatedStatement> createDetail(
            @PathVariable @Parameter(name = "entity identifier") String id,
            @PathVariable(required = true, value = "values") @Parameter(name = "property type") PropertyType type,
            @PathVariable String prefixedValueKey,
            @PathVariable String prefixedDetailKey,
            @RequestBody String value
    );

    @Operation(summary = "Creates a statement about a statement use the post body as value.")
    @PostMapping(value = "/entities/{id:[\\w|\\d|\\-|\\_]+}/{type}/{prefixedValueKey:[\\w|\\d]+\\.[\\w|\\d]+}/details",
            consumes = RdfMimeTypes.TURTLESTAR_VALUE,
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<AnnotatedStatement> createLinkDetail(
            @PathVariable @Parameter(name = "entity identifier") String id,
            @PathVariable(required = true, value = "values") @Parameter(name = "property type") DetailsAPI.PropertyType type,
            @PathVariable String prefixedValueKey,
            @RequestBody Triples request
    );
}
