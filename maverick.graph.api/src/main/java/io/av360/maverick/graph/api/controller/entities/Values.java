package io.av360.maverick.graph.api.controller.entities;

import io.av360.maverick.graph.api.controller.AbstractController;
import io.av360.maverick.graph.model.enums.RdfMimeTypes;
import io.av360.maverick.graph.model.rdf.NamespaceAwareStatement;
import io.av360.maverick.graph.services.EntityServices;
import io.av360.maverick.graph.services.SchemaServices;
import io.av360.maverick.graph.services.ValueServices;
import io.av360.maverick.graph.store.rdf.models.TripleModel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import javax.annotation.Nullable;

@RestController
@RequestMapping(path = "/api/entities")
//@Api(tags = "Values")
@Slf4j(topic = "graph.ctrl.api.values")
@SecurityRequirement(name = "api_key")
public class Values extends AbstractController {


    protected final ValueServices values;

    protected final EntityServices entities;

    protected final SchemaServices schemaServices;

    public Values(ValueServices values, EntityServices entities, SchemaServices schemaServices) {
        this.values = values;
        this.entities = entities;
        this.schemaServices = schemaServices;
    }
    @Operation(summary = "Returns a list of value property of the selected entity.  ")
    @GetMapping(value = "/{id:[\\w|\\d|-|_]+}/values",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public Flux<NamespaceAwareStatement> listEntityValues(@PathVariable String id) {
      return Flux.error(new NotImplementedException("Listing the values has not been implemented yet."));
    }



    //  @ApiOperation(value = "Sets a value for an entity. Replaces an existing value. ")
    @PostMapping(value = "/{id:[\\w|\\d|-|_]+}/values/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public Flux<NamespaceAwareStatement> create(@PathVariable String id, @PathVariable String prefixedKey, @RequestBody String value, @Nullable @RequestParam(required = false) String lang) {
        Assert.isTrue(!value.matches("(?s).*[\\n\\r].*"), "Newlines in request body are not supported");


        return super.getAuthentication()
                .flatMap(authentication -> values.insert(id, prefixedKey, value, lang, authentication))
                .flatMapIterable(TripleModel::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled())
                        log.debug("Request to set property '{}' of entity '{}' to value '{}'", prefixedKey, id, value.length() > 64 ? value.substring(0, 64) : value);
                });
    }


    @Operation(summary = "Create or update multiple value properties for the selected entity.")
    @PostMapping(value = "/{id:[\\w|\\d|-|_]+}/values",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public Flux<NamespaceAwareStatement> listEntityValues(@PathVariable String id, @RequestBody String value) {
        return Flux.error(new NotImplementedException("Updating multiple values has not been implemented yet."));
    }

    @Operation(summary = "Removes a property value.")
    @DeleteMapping(value = "/{id:[\\w|\\d|-|_]+}/values/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public Flux<NamespaceAwareStatement> delete(@PathVariable String id, @PathVariable String prefixedKey, @RequestParam(required = false) String lang) {


        return super.getAuthentication()
                .flatMap(authentication -> values.remove(id, prefixedKey, lang, authentication))
                .flatMapIterable(TripleModel::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled()) log.debug("Deleted property '{}' of entity '{}'", prefixedKey, id);
                });

    }


}

