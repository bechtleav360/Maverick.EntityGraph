package io.av360.maverick.graph.feature.applications.api.ext;

import io.av360.maverick.graph.api.controller.AbstractController;
import io.av360.maverick.graph.api.controller.entities.Values;
import io.av360.maverick.graph.model.enums.RdfMimeTypes;
import io.av360.maverick.graph.model.rdf.NamespaceAwareStatement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import javax.annotation.Nullable;

@RestController
@RequestMapping(path = "/api")
@Slf4j(topic = "graph.ctrl.api.values")
@SecurityRequirement(name = "api_key")
public class ScopedValues extends AbstractController {


    private final Values defaultCtrl;

    public ScopedValues(Values defaultCtrl) {
        this.defaultCtrl = defaultCtrl;
    }
    @Operation(summary = "Returns a list of value properties of the selected entity.  ")
    @GetMapping(value = "/sc/{scope}/entities/{id:[\\w|\\d|-|_]+}/values",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> listEntityValues(@PathVariable String id) {
      return this.defaultCtrl.listEntityValues(id);
    }

    @PostMapping(value = "/sc/{scope}/entities/{id:[\\w|\\d|-|_]+}/values/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> create(@PathVariable String id, @PathVariable String prefixedKey, @RequestBody String value, @Nullable @RequestParam(required = false) String lang) {
        return this.defaultCtrl.create(id, prefixedKey, value, lang);
    }


    @Operation(summary = "Create or update multiple value properties for the selected entity.")
    @PostMapping(value = "/sc/{scope}/entities/{id:[\\w|\\d|-|_]+}/values",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> listEntityValues(@PathVariable String id, @RequestBody String value) {
        return this.defaultCtrl.listEntityValues(id, value);
    }

    @Operation(summary = "Removes a property value.")
    @DeleteMapping(value = "/sc/{scope}/entities/{id:[\\w|\\d|-|_]+}/values/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> delete(@PathVariable String id, @PathVariable String prefixedKey, @RequestParam(required = false) String lang) {
        return this.defaultCtrl.delete(id, prefixedKey, lang);

    }


}

