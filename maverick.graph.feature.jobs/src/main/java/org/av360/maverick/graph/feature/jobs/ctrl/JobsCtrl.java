package org.av360.maverick.graph.feature.jobs.ctrl;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.api.controller.AbstractController;
import org.av360.maverick.graph.feature.jobs.services.DetectDuplicatesService;
import org.av360.maverick.graph.feature.jobs.services.ReplaceExternalIdentifiersServiceV2;
import org.av360.maverick.graph.feature.jobs.services.TypeCoercionService;
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
    final ReplaceExternalIdentifiersServiceV2 replaceExternalIdentifiersService;

    final TypeCoercionService typeCoercionService;

    final SchemaServices schemaServices;

    public JobsCtrl(DetectDuplicatesService detectDuplicatesJob, ReplaceExternalIdentifiersServiceV2 replaceExternalIdentifiersJob, TypeCoercionService typeCoercionService, SchemaServices schemaServices) {
        this.detectDuplicatesJob = detectDuplicatesJob;
        this.replaceExternalIdentifiersService = replaceExternalIdentifiersJob;
        this.typeCoercionService = typeCoercionService;
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

    @PostMapping(value = "/execute/normalize")
    @ResponseStatus(HttpStatus.OK)
    Mono<Void> execSkolemizationJob() {
        return super.getAuthentication()
                .flatMapMany(this.replaceExternalIdentifiersService::checkForExternalIdentifiers).then();

    }


    @PostMapping(value = "/execute/coercion")
    @ResponseStatus(HttpStatus.OK)
    Mono<Void> execCoercionJob() {
        return super.getAuthentication()
                .flatMapMany(this.typeCoercionService::run).then();

    }


}
