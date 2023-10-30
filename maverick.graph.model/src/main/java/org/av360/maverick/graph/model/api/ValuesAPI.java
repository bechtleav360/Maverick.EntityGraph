package org.av360.maverick.graph.model.api;

import io.swagger.v3.oas.annotations.Operation;
import org.av360.maverick.graph.model.enums.RdfMimeTypes;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import javax.annotation.Nullable;

public interface ValuesAPI {
    @Operation(summary = "Returns a list of value property of the selected entity.  ")
    @GetMapping(value = "/entities/{id:[\\w|\\d|\\-|\\_]+}/values",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<AnnotatedStatement> list(@PathVariable String id, @Nullable @RequestParam(required = false) String prefixedKey);

    //  @ApiOperation(value = "Sets a value for an entity. Replaces an existing value. ")
    @PostMapping(value = "/entities/{id:[\\w|\\d|\\-|\\_]+}/values/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d|\\-|\\_]+}",
            consumes = {MediaType.TEXT_PLAIN_VALUE},
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<AnnotatedStatement> create(@PathVariable String id,
                                    @PathVariable String prefixedKey,
                                    @RequestBody String value,
                                    @Nullable @RequestParam(required = false) String lang,
                                    @Nullable @RequestParam(required = false) Boolean replace);



    @Operation(summary = "Removes a property value.")
    @DeleteMapping(value = "/entities/{id:[\\w|\\d|\\-|\\_]+}/values/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<AnnotatedStatement> delete(@PathVariable String id, @PathVariable String prefixedKey, @RequestParam(required = false) String lang);
}
