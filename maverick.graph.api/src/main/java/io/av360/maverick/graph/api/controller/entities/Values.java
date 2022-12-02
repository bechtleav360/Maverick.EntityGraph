package io.av360.maverick.graph.api.controller.entities;

import io.av360.maverick.graph.api.controller.AbstractController;
import io.av360.maverick.graph.model.enums.RdfMimeTypes;
import io.av360.maverick.graph.model.rdf.NamespaceAwareStatement;
import io.av360.maverick.graph.services.EntityServices;
import io.av360.maverick.graph.services.ValueServices;
import io.av360.maverick.graph.store.rdf.models.AbstractModel;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping(path = "/api/entities")
//@Api(tags = "Values")
@Slf4j(topic = "graph.api.entities")
@SecurityRequirement(name = "api_key")
public class Values extends AbstractController {


    protected final ValueServices values;

    protected final EntityServices entities;

    public Values(ValueServices values, EntityServices entities) {
        this.values = values;
        this.entities = entities;
    }

    //  @ApiOperation(value = "Sets a value for an entity. Replaces an existing value. ")
    @PostMapping(value = "/{id:[\\w|\\d|-|_]+}/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> create(@PathVariable String id, @PathVariable String prefixedKey, @RequestBody String value) {
        Assert.isTrue(!value.matches("(?s).*[\\n\\r].*"), "Newlines in request body are not supported");

        String[] property = splitPrefixedIdentifier(prefixedKey);
        return super.getAuthentication()
                .flatMap(authentication ->  values.insertValue(id, property[0], property[1], value, authentication))
                .flatMapIterable(AbstractModel::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled()) log.debug("Request to set property '{}' of entity '{}' to value '{}'", prefixedKey, id, value.length() > 64 ? value.substring(0, 64) : value);
                });

    }

    //@ApiOperation(value = "Removes value")
    @DeleteMapping(value = "/{id:[\\w|\\d|-|_]+}/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<NamespaceAwareStatement> delete(@PathVariable String id, @PathVariable String prefixedKey, @RequestParam(required = false) String lang) {

        String[] property = splitPrefixedIdentifier(prefixedKey);

        return super.getAuthentication()
                .flatMap(authentication ->  values.removeValue(id, property[0], property[1], lang, authentication))
                .flatMapIterable(AbstractModel::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled()) log.debug("Deleted property '{}' of entity '{}'", prefixedKey, id);
                });

    }



}

