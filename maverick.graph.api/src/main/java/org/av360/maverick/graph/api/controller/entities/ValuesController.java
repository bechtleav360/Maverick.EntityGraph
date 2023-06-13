package org.av360.maverick.graph.api.controller.entities;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.av360.maverick.graph.api.controller.AbstractController;
import org.av360.maverick.graph.model.api.ValuesAPI;
import org.av360.maverick.graph.model.enums.RdfMimeTypes;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.services.EntityServices;
import org.av360.maverick.graph.services.SchemaServices;
import org.av360.maverick.graph.services.ValueServices;
import org.av360.maverick.graph.store.rdf.fragments.TripleModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import javax.annotation.Nullable;

@RestController
@RequestMapping(path = "/api")
//@Api(tags = "Values")
@Slf4j(topic = "graph.ctrl.api.values")
@SecurityRequirement(name = "api_key")
@Tag(name = "Annotations")
public class ValuesController extends AbstractController implements ValuesAPI {


    protected final ValueServices values;

    protected final EntityServices entities;
    protected final SchemaServices schemaServices;

    public ValuesController(ValueServices values, EntityServices entities, SchemaServices schemaServices) {
        this.values = values;
        this.entities = entities;
        this.schemaServices = schemaServices;
    }
    @Override
    @Operation(summary = "Returns a list of value property of the selected entity.  ")
    @GetMapping(value = "/entities/{id:[\\w|\\d|\\-|\\_]+}/values",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public Flux<AnnotatedStatement> listEntityValues(@PathVariable String id) {
      return Flux.error(new NotImplementedException("Listing the values has not been implemented yet."));
    }



    //  @ApiOperation(value = "Sets a value for an entity. Replaces an existing value. ")
    @Override
    @PostMapping(value = "/entities/{id:[\\w|\\d|\\-|\\_]+}/values/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d|\\-|\\_]+}",
            consumes = {MediaType.TEXT_PLAIN_VALUE},
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public Flux<AnnotatedStatement> create(@PathVariable String id, @PathVariable String prefixedKey, @RequestBody String value, @Nullable @RequestParam(required = false) String lang) {
        Assert.isTrue(!value.matches("(?s).*[\\n\\r].*"), "Newlines in request body are not supported");


        return super.acquireContext()
                .flatMap(ctx -> values.insertLiteral(id, prefixedKey, value, lang, ctx))
                .flatMapIterable(TripleModel::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled())
                        log.debug("Request to set property '{}' of entity '{}' to value '{}'", prefixedKey, id, value.length() > 64 ? value.substring(0, 64) : value);
                });
    }


    @Override
    @Operation(summary = "Create or update multiple value properties for the selected entity.")
    @PostMapping(value = "/entities/{id:[\\w|\\d|\\-|\\_]+}/values",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public Flux<AnnotatedStatement> listEntityValues(@PathVariable String id, @RequestBody String value) {
        return Flux.error(new NotImplementedException("Updating multiple values has not been implemented yet."));
    }

    @Override
    @Operation(summary = "Removes a property value.")
    @DeleteMapping(value = "/entities/{id:[\\w|\\d|\\-|\\_]+}/values/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public Flux<AnnotatedStatement> delete(@PathVariable String id, @PathVariable String prefixedKey, @RequestParam(required = false) String lang) {


        return super.acquireContext()
                .flatMap(ctx -> values.removeLiteral(id, prefixedKey, lang, ctx))
                .flatMapIterable(TripleModel::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled()) log.debug("Deleted property '{}' of entity '{}'", prefixedKey, id);
                });

    }


}

