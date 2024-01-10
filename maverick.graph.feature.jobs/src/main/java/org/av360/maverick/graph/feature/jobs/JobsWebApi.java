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

package org.av360.maverick.graph.feature.jobs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import reactor.core.publisher.Mono;

@Tag(name = "Tools for controlling jobs.", description = """
        ### Methods to execute internal graph maintenance jobs manually. 
        \s
        The jobs are typically scheduled internally. This can be manually deactivated, in this case they should be triggered through this API. 
        """,
        extensions = @Extension(name = "order", properties = {@ExtensionProperty(name = "position", value = "2")}))
@RequestMapping(path = "/api/admin/jobs")
@SecurityRequirement(name = "api_key")
public interface JobsWebApi {
    @PostMapping(value = "/execute/deduplication")
    @Operation(summary = "Executes the deduplication job. Two entities are duplicates if they are the same, that means have the same identity. Identity is identified through characteristic properties. ")
    @ResponseStatus(HttpStatus.OK)
    Mono<Void> execDeduplicationJob();

    @PostMapping(value = "/execute/normalize/subjectIdentifiers")
    @Operation(summary = "Executes the job for normalizing subject identifiers. Blank nodes and external identifiers imported from external sources are typically using internal identifiers (which means we cannot " +
            "address them through this API). These external identifiers and blank nodes are replaced. External identifiers are stored under the <owl:sameAs> relation.")
    @ResponseStatus(HttpStatus.ACCEPTED)
    Mono<Void> execReplaceSubjectIdentifiersJob();

    @PostMapping(value = "/execute/normalize/objectIdentifiers")
    @Operation(summary = "Executes the job for normalizing object identifiers. Blank nodes and external identifiers imported from external sources are typically using internal identifiers (which means we cannot " +
            "address them through this API). External identifiers without an existing subject in the graph will not be modified. ")
    @ResponseStatus(HttpStatus.ACCEPTED)
    Mono<Void> execReplaceObjectIdentifiersJob();

    @PostMapping(value = "/execute/coercion")
    @Operation(summary = "Executes the job for assigning internal types to fragments. Does usually happen automatically, but not during the import. ")
    @ResponseStatus(HttpStatus.OK)
    Mono<Void> execCoercionJob();

    @PostMapping(value = "/execute/export")
    @Operation(summary = "Executes the job for exporting the content of an repository to the file system.")
    @ResponseStatus(HttpStatus.ACCEPTED)
    Mono<Void> execExportJob(@RequestParam(required = false, defaultValue = "entities", value = "entities") @Parameter(name = "repository", description = "The repository type in which the query should search.")
                             RepositoryType repositoryType);
}
