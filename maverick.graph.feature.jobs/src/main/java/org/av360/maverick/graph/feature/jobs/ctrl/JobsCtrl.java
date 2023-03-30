package org.av360.maverick.graph.feature.jobs.ctrl;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.api.controller.AbstractController;
import org.av360.maverick.graph.feature.jobs.services.DetectDuplicatesService;
import org.av360.maverick.graph.feature.jobs.services.ReplaceExternalIdentifiersService;
import org.av360.maverick.graph.services.SchemaServices;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(path = "/api/admin/jobs")
//@Api(tags = "Admin Operations")
@Slf4j(topic = "graph.feat.admin.ctrl.api")
@SecurityRequirement(name = "api_key")
public class JobsCtrl extends AbstractController {



    final DetectDuplicatesService detectDuplicatesJob;
    final ReplaceExternalIdentifiersService replaceExternalIdentifiersJob;

    final SchemaServices schemaServices;

    public JobsCtrl(DetectDuplicatesService detectDuplicatesJob, ReplaceExternalIdentifiersService replaceExternalIdentifiersJob, SchemaServices schemaServices) {
        this.detectDuplicatesJob = detectDuplicatesJob;
        this.replaceExternalIdentifiersJob = replaceExternalIdentifiersJob;
        this.schemaServices = schemaServices;
    }


    //@ApiOperation(value = "Empty repository", tags = {})
    @PostMapping(value = "/execute/deduplication")
    @ResponseStatus(HttpStatus.OK)
    Mono<Void> execDeduplicationJob(
            @RequestParam(name = "property", required = true)
            @Parameter(
                    schema = @Schema(type = "string",
                            allowableValues = {"dc.identifier", "dcterms.identifier", "sdo.identifier", "rdfs.label", "skos.pref_label"})
                    )
            String property) {

        return Mono.zip(
                super.getAuthentication(),
                schemaServices.resolvePrefixedName(property)
        ).flatMap(tuple -> this.detectDuplicatesJob.checkForDuplicates(tuple.getT2(), tuple.getT1()));

    }

    @PostMapping(value = "/execute/skolemization")
    @ResponseStatus(HttpStatus.OK)
    Mono<Void> execSkolemizationJob() {
        return super.getAuthentication()
                .flatMapMany(this.replaceExternalIdentifiersJob::checkForGlobalIdentifiers).then();

    }


}
