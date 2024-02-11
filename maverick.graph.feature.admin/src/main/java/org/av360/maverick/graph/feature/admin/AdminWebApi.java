/*
 * Copyright (c) 2024.
 *
 *  Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 *  European Commission - subsequent versions of the EUPL (the "Licence");
 *
 *  You may not use this work except in compliance with the Licence.
 *  You may obtain a copy of the Licence at:
 *
 *  https://joinup.ec.europa.eu/software/page/eupl5
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the Licence for the specific language governing permissions and limitations under the Licence.
 *
 */

package org.av360.maverick.graph.feature.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.av360.maverick.graph.feature.admin.controller.dto.ImportFromEndpointRequest;
import org.av360.maverick.graph.model.enums.RdfMimeTypes;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Tag(name = "Import and Export Tools", description = """
        ### Methods to administrate the Entity Graph
        \s
        Use these methods to import or reset content.
        """,
        extensions = @Extension(name = "order", properties = {@ExtensionProperty(name = "position", value = "1")}))
@RequestMapping(path = "/api/admin")
@SecurityRequirement(name = "api_key")
public interface AdminWebApi {
    //@ApiOperation(value = "Empty repository", tags = {})
    @GetMapping(value = "/reset")
    @Operation(summary = "Removes all statements within the requested repository")
    @ResponseStatus(HttpStatus.ACCEPTED)
    Mono<Void> resetRepository(
            @RequestParam(required = false, defaultValue = "entities", value = "entities") @Parameter(name = "repository", description = "The repository type in which the query should search.")
            RepositoryType repositoryType);

    @PostMapping(value = "/import/content", consumes = {RdfMimeTypes.JSONLD_VALUE, RdfMimeTypes.NTRIPLES_VALUE, RdfMimeTypes.TURTLE_VALUE, RdfMimeTypes.N3_VALUE, RdfMimeTypes.RDFXML_VALUE, RdfMimeTypes.BINARY_VALUE, RdfMimeTypes.NQUADS_VALUE, RdfMimeTypes.TURTLESTAR_VALUE})
    @Operation(summary = "Imports rdf content in request body into the target repository")
    @ResponseStatus(HttpStatus.ACCEPTED)
    Mono<Void> importEntities(
            @RequestBody @Parameter(name = "data", description = "The rdf data.") Flux<DataBuffer> bytes,
            // @ApiParam(example = "text/turtle")
            @RequestParam(required = false, defaultValue = "entities", value = "entities") @Parameter(name = "repository", description = "The repository type in which the query should search.")
            RepositoryType repositoryType,
            @RequestHeader(HttpHeaders.CONTENT_TYPE)
            @Parameter(description = "The RDF format of the content", schema = @Schema(type = "string", allowableValues = {"text/turtle", "application/n3", "application/n-triples", "application/rdf+xml", "application/ld+json", "application/n-quads", "application/vnd.hdt"}))
            String mimetype
    );

    @PostMapping(value = "/import/endpoint", consumes = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "Imports rdf content from sparql endpoint into target repository")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Metadata for the external endpoint",
            content = @Content(examples = {
                    @ExampleObject(name = "Import from sandbox", value = """
                            {
                              "endpoint": "https://entitygraph.azurewebsites.net/api/query/select?repository=entities",
                              "headers": {
                                "X-Application": "default",
                                "X-API-KEY": "the_api_key"
                              }
                            }
                        """)
            })
    )
    @ResponseStatus(HttpStatus.ACCEPTED)
    Mono<Void> importFromSparql(
            @RequestBody @Parameter(name = "endpoint", description = "URL to the sparql endpoint.") ImportFromEndpointRequest importFromEndpointRequest,
            @RequestParam(required = false, defaultValue = "entities", value = "entities") @Parameter(name = "repository", description = "The repository type to import to.")
            RepositoryType repositoryType
    );

    @PostMapping(value = "/import/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Imports rdf content from file into target repository")
    @ResponseStatus(HttpStatus.ACCEPTED)
    Mono<Void> importFile(
            @RequestPart
            @Parameter(name = "file", description = "The file with rdf data.") Mono<FilePart> fileMono,
            //@ApiParam(example = "text/turtle")
            @RequestParam(required = false, defaultValue = "entities", value = "entities")
            @Parameter(name = "repository", description = "The repository type in which the query should search.")
            RepositoryType repositoryType,
            @RequestParam
            @Parameter(description = "The RDF format of the file", schema = @Schema(type = "string", allowableValues = {"text/turtle", "application/rdf+xml", "application/ld+json", "application/n-quads", "application/vnd.hdt"}))
            String mimetype);

    @PostMapping(value = "/import/package", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Importing a zipped file. NOT IMPLEMENTED YET")
    @ResponseStatus(HttpStatus.ACCEPTED)
    Mono<Void> importPackage(
            @RequestPart
            @Parameter(name = "file", description = "The zip file.") Mono<FilePart> fileMono,
            @RequestParam(required = false, defaultValue = "entities", value = "entities")
            @Parameter(name = "repository", description = "The repository type in which the query should search.")
            RepositoryType repositoryType);
}
