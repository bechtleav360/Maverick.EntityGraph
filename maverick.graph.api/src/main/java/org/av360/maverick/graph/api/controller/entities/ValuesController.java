package org.av360.maverick.graph.api.controller.entities;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.api.controller.AbstractController;
import org.av360.maverick.graph.model.api.ValuesAPI;
import org.av360.maverick.graph.model.enums.RdfMimeTypes;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.model.rdf.Triples;
import org.av360.maverick.graph.services.EntityServices;
import org.av360.maverick.graph.services.SchemaServices;
import org.av360.maverick.graph.services.ValueServices;
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
    @Operation(summary = "Returns a list of value properties of the selected entity.  ",
            description = """
                    Returns a list of values formatted as RDF Star. This operation will include the details (as well as the hashes), required to select individual values for processing. 
                    Please verify that you can actually parse this syntax. 
                    
                    """)
    @GetMapping(value = "/entities/{id:[\\w|\\d|\\-|\\_]+}/values",
            produces = {RdfMimeTypes.TURTLESTAR_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public Flux<AnnotatedStatement> list(@PathVariable String id, @Nullable @RequestParam(required = false) String property) {

        return super.acquireContext()
                .flatMap(ctx -> values.listValues(id, property, ctx))
                .flatMapIterable(Triples::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled())
                        log.debug("Request to list values of entity '{}'", id);
                });
    }



    //  @ApiOperation(value = "Sets a value for an entity. Replaces an existing value. ")
    @Override
    @Operation(summary = "Sets a specific value.  ",
            description = """
                You can either set literals or links to resources with this method. 
                
                A literal is a specific type of value that represents data in a straightforward and self-contained 
                manner. It can be a string, a number, a date/time value, a boolean, or any other type that can be
                expressed in a textual format. In RDF, literals play a crucial role in representing and exchanging
                structured data, allowing for semantic understanding and integration of information
                from various sources. To set a literal, simply add any value in a single line in the request body.  
                
                Resources are identified by URIs (Uniform Resource Identifiers), which serve as globally unique
                identifiers for each resource. URIs can take the form of URLs (Uniform Resource Locators)
                or URNs (Uniform Resource Names). To set a link to a resource, you have to insert either a URN or URL 
                in tags "<,>" in the request body. 
                
                The request body has to be short, line breaks are not supported. For content, please upload the value 
                as a file (if the feature is enabled)  
                
                    """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "The value to set",
            content = @Content(examples = {
                    @ExampleObject(name = "Set value as link to a resource", value = "<https://www.wikidata.org/wiki/Q42>"),
                    @ExampleObject(name = "Set value as link to a webpage", value = "https://bar.wikipedia.org/wiki/Douglas_Adams"),
                    @ExampleObject(name = "Set value as literal", value = "Douglas Adams"),
            })
    )
    @PostMapping(value = "/entities/{id:[\\w|\\d|\\-|\\_]+}/values/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d|\\-|\\_]+}",
            consumes = {MediaType.TEXT_PLAIN_VALUE},
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public Flux<AnnotatedStatement> create(@PathVariable String id,
                                           @PathVariable String prefixedKey,
                                           @RequestBody String value,
                                           @Nullable @RequestParam(required = false) String lang,
                                           @Nullable @RequestParam(required = false) Boolean replace) {
        Assert.isTrue(!value.matches("(?s).*[\\n\\r].*"), "Newlines in request body are not supported");

        return super.acquireContext()
                .flatMap(ctx -> values.insertValue(id, prefixedKey, value, lang, replace, ctx))
                .flatMapIterable(Triples::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled())
                        log.debug("Request to set property '{}' of entity '{}' to value '{}'", prefixedKey, id, value.length() > 64 ? value.substring(0, 64) : value);
                });
    }




    @Override
    @Operation(summary = "Removes a property value.",
            description = """
                If you have multiple values for a given property, you need to select a specific value either by language tag (if it is unique) or by hash. 
                Retrieve the hashes by calling the operation
                
                    """)
    @DeleteMapping(value = "/entities/{id:[\\w|\\d|\\-|\\_]+}/values/{prefixedKey:[\\w|\\d]+\\.[\\w|\\d]+}",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public Flux<AnnotatedStatement> delete(@PathVariable String id, @PathVariable String prefixedKey, @RequestParam(required = false) String lang, @RequestParam(required = false) String identifier) {


        return super.acquireContext()
                .flatMap(ctx -> values.removeLiteral(id, prefixedKey, lang, identifier, ctx))
                .flatMapIterable(Triples::asStatements)
                .doOnSubscribe(s -> {
                    if (log.isDebugEnabled()) log.debug("Deleted property '{}' of entity '{}'", prefixedKey, id);
                });

    }


}

