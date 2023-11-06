package org.av360.maverick.graph.model.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.av360.maverick.graph.model.enums.RdfMimeTypes;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.model.rdf.Triples;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import javax.annotation.Nullable;

@RequestMapping(path = "/api")
//@Api(tags = "Values")
@SecurityRequirement(name = "api_key")
@Tag(name = "Values", description = """
        ### Manage the value properties (literals) for an Entity. 
        \s
        In RDF (Resource Description Framework), a **literal** represents a primitive value, such as a string, number,
        or date. Unlike resources, which have URIs, literals are terminal values in RDF triples. They typically act
        as the object of a statement, directly providing the value or attribute of a subject-resource without the need
        for dereferencing a URI. Common examples include string values with optional language tags, numerical values,
        and date-time values.
        \s
        Values can be annotated with additional metadata, such as provenance **details** highlighting their origin or
        assertions about their trustworthiness. Each value is equipped with a distinct identifier, enabling precise
        pinpointing for certain operations. Furthermore, values can manifest as composite structures, captured by a
        group of interconnected RDF statements, like those representing addresses. When the parent
        Entity is deleted, all its corresponding values are simultaneously removed.
        \s 
        You can also store objects (binary files, large text documents, etc.) as values to an entity via file upload. 
        They are persisted in an object store. Adresses, compound names, etc. should be stored as composites. 
        
        """)
public interface ValuesAPI {
    @Operation(
            operationId = "listValues",
            summary = "Returns a list of values of the selected entity identified by the Entity Key.",
            description = """
                    This operation retrieves a list of values in the RDF Star format. If detailed information is present
                    for a value, it will be included in the response. Additionally, checksums are generated for each value,
                    essential for subsequent operations that modify duplicates or append details. Ensure your parsers
                    support the Turtle Star format before processing.
                    \s
                    The result can be further filtered by a given prefixed property, such as "sdo.teaches" for https://schema.org/teaches. 
                                        """,
            parameters = {
                    @Parameter(name = "key", description = "Unique identifier for the entity", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string")),
                    @Parameter(name = "prefixedProperty", description = "Optional prefixed property as filter for values", required = false, in = ParameterIn.QUERY, schema = @Schema(type = "string"))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved list of values", content = @Content(mediaType = RdfMimeTypes.TURTLESTAR_VALUE, schema = @Schema(implementation = AnnotatedStatement.class)))
            }
    )
    @GetMapping(value = "/entities/{key:[\\w|\\d|\\-|\\_]+}/values", produces = {RdfMimeTypes.TURTLESTAR_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<AnnotatedStatement> list(@PathVariable String key,
                                  @Nullable @RequestParam(required = false) String prefixedKey);

    //  @ApiOperation(value = "Sets a value for an entity. Replaces an existing value. ")
    @Operation(
            operationId = "insertValue",
            summary = "Sets a specific value for the entity identified by the Entity Key.  ",
            description = """
                    You can either set literals or links to external resources with this method.
                    \s
                    A literal is a specific type of value that represents data in a straightforward and self-contained 
                    manner. It can be a string, a number, a date/time value, a boolean, or any other type that can be
                    expressed in a textual format. In RDF, literals play a crucial role in representing and exchanging
                    structured data, allowing for semantic understanding and integration of information
                    from various sources. To set a literal, simply add any value in a single line in the request body.  
                    \s
                    Resources are identified by URIs (Uniform Resource Identifiers), which serve as globally unique
                    identifiers for each resource. URIs can take the form of URLs (Uniform Resource Locators)
                    or URNs (Uniform Resource Names). To set a link to a resource, you have to insert either a URN or URL 
                    in tags "<,>" in the request body. 
                    \s
                    The request body has to be short, line breaks are not supported. For content, please upload the value 
                    as a file (if the feature is enabled)  
                    """,
            parameters = {
                    @Parameter(name = "key", description = "Unique identifier for the entity", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string")),
                    @Parameter(name = "prefixedProperty", description = "Prefixed property for the entity value", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string")),
                    @Parameter(name = "languageTag", description = "Language tag for the value", required = false, in = ParameterIn.QUERY, schema = @Schema(type = "string")),
                    @Parameter(name = "replace", description = "Should an existing value be replaced, or a new one added", required = false, in = ParameterIn.QUERY, schema = @Schema(type = "boolean"))
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "The value to set", required = true, content = {
                    @Content(examples = {
                            @ExampleObject(name = "Set value as link to a resource", value = "<https://www.wikidata.org/wiki/Q42>"),
                            @ExampleObject(name = "Set value as link to a webpage", value = "https://bar.wikipedia.org/wiki/Douglas_Adams"),
                            @ExampleObject(name = "Set value as literal", value = "Douglas Adams"),
                    })
            })
    )
    @PostMapping(value = "/entities/{key:[\\w|\\d|\\-|\\_]+}/values/{prefixedProperty:[\\w|\\d]+\\.[\\w|\\d|\\-|\\_]+}",
            consumes = {MediaType.TEXT_PLAIN_VALUE},
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<AnnotatedStatement> insert(@PathVariable String key,
                                    @PathVariable String prefixedProperty,
                                    @RequestBody String value,
                                    @Nullable @RequestParam(required = false) String languageTag,
                                    @Nullable @RequestParam(required = false) Boolean replace);


    @Operation(
            operationId = "removeValue",
            summary = "Removes an entity value.",
            description = """
                    This operation deletes a specific entity value identified by its key and prefixed property. If
                    multiple values are associated with the given property, you must provide either a language
                    tag (e.g., ""@en for English, which is the default) or a system-generated value identifier
                    for precise deletion. Language tags are applicable only for string literals. In cases
                    where the literal isn't a string or where language tags aren't unique, specifying the value
                    identifier is mandatory. This identifier is an on-the-fly system-generated checksum.
                    To fetch these identifiers, utilize the listValues method.
                    \s                    
                    Will also remove all details associated with this value. 
                    """,
            parameters = {
                    @Parameter(name = "key", description = "Unique key for the entity", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string")),
                    @Parameter(name = "prefixedProperty", description = "Prefixed property for the entity value", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string")),
                    @Parameter(name = "languageTag", description = "Language tag for the value", required = false, in = ParameterIn.QUERY, schema = @Schema(type = "string")),
                    @Parameter(name = "valueIdentifier", description = "Identifier for the specific value", required = false, in = ParameterIn.QUERY, schema = @Schema(type = "string"))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully deleted the entity value", content = {
                            @Content(mediaType = RdfMimeTypes.TURTLE_VALUE, schema = @Schema(implementation = AnnotatedStatement.class)),
                            @Content(mediaType = RdfMimeTypes.JSONLD_VALUE, schema = @Schema(implementation = AnnotatedStatement.class))
                    })
            }
    )
    @DeleteMapping(value = "/entities/{key:[\\w|\\d|\\-|\\_]+}/values/{prefixedProperty:[\\w|\\d]+\\.[\\w|\\d]+}",
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.OK)
    Flux<AnnotatedStatement> remove(@PathVariable String key,
                                    @PathVariable String prefixedProperty,
                                    @RequestParam(required = false) String languageTag,
                                    @RequestParam(required = false) String valueIdentifier);


    @Operation(
            summary = "Embeds provided triples to an entity. Returns transaction information.",
            description = """
                    This operation facilitates the embedding of triples into a specific entity, distinguished by its key
                    and prefixed property key (such as sdo.publisher for https://schema.org/publisher). While it supports
                    the addition of composite values to entities, deeper nesting is restricted due to the limitation in
                    inferring a relation's destination from a statement set containing multiple subjects.
                    """,
            parameters = {
                    @Parameter(name = "key", description = "Unique identifier for the entity", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string")),
                    @Parameter(name = "prefixedProperty", description = "Prefixed property key used to link the entity to the given triples", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string"))
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Triples to embed", required = true, content = {
                    @Content(mediaType = RdfMimeTypes.JSONLD_VALUE, schema = @Schema(implementation = Triples.class)),
                    @Content(mediaType = RdfMimeTypes.TURTLE_VALUE, schema = @Schema(implementation = Triples.class))
            }),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Successfully embedded triples", content = {
                            @Content(mediaType = RdfMimeTypes.TURTLE_VALUE, schema = @Schema(implementation = AnnotatedStatement.class)),
                            @Content(mediaType = RdfMimeTypes.JSONLD_VALUE, schema = @Schema(implementation = AnnotatedStatement.class))
                    })
            }
    )
    @PostMapping(value = "/entities/{key:[\\w|\\d|\\-|\\_]+}/composites/{prefixedProperty:[\\w|\\d]+\\.[\\w|\\d]+}",
            consumes = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.TURTLE_VALUE},
            produces = {RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.JSONLD_VALUE})
    @ResponseStatus(HttpStatus.CREATED)
    Flux<AnnotatedStatement> embed(@PathVariable String key, @PathVariable String prefixedProperty, @RequestBody Triples value);
}
