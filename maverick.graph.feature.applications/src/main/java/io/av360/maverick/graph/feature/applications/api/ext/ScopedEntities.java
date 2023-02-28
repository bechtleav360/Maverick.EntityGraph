package io.av360.maverick.graph.feature.applications.api.ext;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.av360.maverick.graph.api.controller.entities.Entities;
import io.av360.maverick.graph.feature.applications.domain.ApplicationsService;
import io.av360.maverick.graph.model.enums.RdfMimeTypes;
import io.av360.maverick.graph.model.rdf.GeneratedIdentifier;
import io.av360.maverick.graph.model.rdf.NamespaceAwareStatement;
import io.av360.maverick.graph.services.EntityServices;
import io.av360.maverick.graph.services.QueryServices;
import io.av360.maverick.graph.store.rdf.models.TripleBag;
import io.av360.maverick.graph.store.rdf.models.TripleModel;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 *   Extension to the entities api with the application id in the url (instead of the header). Required for unique urls
 *   (and navigation)
 *
 *   The application parameter in the url is ignored, it is injected into the authentication object by the auth manager
 */
@RestController
@Qualifier("EntityApi")
@Order(2)
@RequestMapping(path = "/api")
@Slf4j(topic = "graph.api.entities")
@OpenAPIDefinition(

)
@SecurityRequirement(name = "api_key")
public class ScopedEntities extends Entities {
    private final ApplicationsService applicationsService;
    public ScopedEntities(ObjectMapper objectMapper, EntityServices graphService, QueryServices queryServices, ApplicationsService applicationsService) {
        super(objectMapper, graphService, queryServices);
        this.applicationsService = applicationsService;
    }

    @GetMapping(value = "/app/{application}/entities/{id:[\\w|\\d|-|_]+}",
            produces = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> readScoped(@PathVariable String applicationLabel, @PathVariable String id) {
        Assert.isTrue(id.length() == GeneratedIdentifier.LENGTH, "Incorrect length for identifier.");

        /*
        return super.getAuthentication()
                .flatMap(authentication -> this.applicationsService.getApplicationByLabel(applicationLabel, authentication))
                .flatMap(authentication -> entityServices.readEntity(id, authentication))
                .flatMapIterable(TripleModel::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled()) log.debug("Request to read entity with id: {}", id);
                });
         */
        return Flux.empty();
    }


    @GetMapping(value = "/app/{application}/entities", produces = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> listScoped(
            @PathVariable String application,
            @RequestParam(value = "limit", defaultValue = "5000") Integer limit,
            @RequestParam(value = "offset", defaultValue = "0") Integer offset) {

        return super.getAuthentication()
                .flatMapMany(authentication -> queryServices.listEntities(authentication, limit, offset))
                .flatMapIterable(TripleModel::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled()) log.debug("Request to list entities");
                });
    }

    @PostMapping(value = "/app/{application}/entities",
            consumes = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE},
            produces = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE})
    @ResponseStatus(HttpStatus.ACCEPTED)
    Flux<NamespaceAwareStatement> createScoped(@PathVariable String application, @RequestBody TripleBag request) {
        Assert.isTrue(request.getModel().size() > 0, "No statements in request detected.");

        return super.getAuthentication()
                .flatMap(authentication -> entityServices.createEntity(request, Map.of(), authentication))
                .flatMapIterable(TripleModel::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled()) log.debug("Request to create a new Entity");
                    if (log.isTraceEnabled()) log.trace("Payload: \n {}", request.toString());
                });
    }

    @PostMapping(value = "/app/{application}/entities/{id:[\\w|\\d|-|_]+}/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}",
            consumes = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE},
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.CREATED)
    Flux<NamespaceAwareStatement> embedScoped(@PathVariable String application, @PathVariable String id, @PathVariable String prefixedKey, @RequestBody TripleBag value) {

        String[] property = splitPrefixedIdentifier(prefixedKey);
        return super.getAuthentication()
                .flatMap(authentication -> entityServices.linkEntityTo(id, property[0], property[1], value, authentication))
                .flatMapIterable(TripleModel::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled())
                        log.debug("Request to add embedded entities as property '{}' to entity '{}'", prefixedKey, id);
                });
    }


    @DeleteMapping(value = "/app/{application}/entities/{id:[\\w|\\d|-|_]+}",
            produces = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> deleteScoped(@PathVariable String application, @PathVariable String id) {
        Assert.isTrue(id.length() == GeneratedIdentifier.LENGTH, "Incorrect length for identifier.");

        return super.getAuthentication()
                .flatMap(authentication -> entityServices.deleteEntity(id, authentication))
                .flatMapIterable(TripleModel::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled()) log.debug("Delete an Entity");
                    if (log.isTraceEnabled()) log.trace("id: \n {}", id);
                });
    }

}
