package io.av360.maverick.graph.feature.applications.api.ext;

import io.av360.maverick.graph.api.controller.AbstractController;
import io.av360.maverick.graph.api.controller.entities.Entities;
import io.av360.maverick.graph.model.enums.RdfMimeTypes;
import io.av360.maverick.graph.model.rdf.NamespaceAwareStatement;
import io.av360.maverick.graph.store.rdf.models.TripleBag;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import javax.annotation.Nullable;

/**
 *   Decorator for the default entities controller with the application label in the url (instead of the header). Required for unique urls
 *   (and navigation)
 *
 *   The application parameter in the url is ignored, it is injected into the authentication object by the auth manager.
 *
 *   @see io.av360.maverick.graph.feature.applications.security.ApplicationAuthenticationManager
 */
@RestController
@RequestMapping(path = "/api")
@Slf4j(topic = "graph.feat.app.ctrl.api.entities.scoped")
@OpenAPIDefinition(
    info =  @Info(title = "Access to entities", description = "Methods to read or manipulate entities"),

        tags = @Tag(name = "Scoped with application label")

)
@SecurityRequirement(name = "api_key")
public class ScopedEntities extends AbstractController {
    private final Entities defaultCtrl;

    public ScopedEntities(Entities defaultCtrl) {
        this.defaultCtrl = defaultCtrl;
    }

    @GetMapping(value = "/sc/{scope}/entities/{id:[\\w|\\d|-|_]+}",
            produces = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> read(@PathVariable String scope, @PathVariable String id, @RequestParam(required = false) @Nullable String property) {
        return defaultCtrl.read(id, property);
    }


    @GetMapping(value = "/sc/{scope}/entities", produces = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> list(
            @PathVariable String scope,
            @RequestParam(value = "limit", defaultValue = "5000") Integer limit,
            @RequestParam(value = "offset", defaultValue = "0") Integer offset) {
        return defaultCtrl.list(limit, offset);
    }

    @PostMapping(value = "/sc/{scope}/entities",
            consumes = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE},
            produces = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE})
    @ResponseStatus(HttpStatus.ACCEPTED)
    Flux<NamespaceAwareStatement> create(@PathVariable String scope, @RequestBody TripleBag request) {
        return defaultCtrl.create(request);
    }

    @PostMapping(value = "/sc/{scope}/entities/{id:[\\w|\\d|-|_]+}/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}",
            consumes = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE},
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.CREATED)
    Flux<NamespaceAwareStatement> embed(@PathVariable String application, @PathVariable String id, @PathVariable String prefixedKey, @RequestBody TripleBag value) {
        return defaultCtrl.embed(id, prefixedKey, value);
    }


    @DeleteMapping(value = "/sc/{scope}/entities/{id:[\\w|\\d|-|_]+}",
            produces = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> delete(@PathVariable String application, @PathVariable String id) {
        return defaultCtrl.delete(id);
    }

}
