package org.av360.maverick.graph.feature.jobs.ctrl;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.api.controller.AbstractController;
import org.av360.maverick.graph.feature.jobs.*;
import org.av360.maverick.graph.services.JobSchedulingService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(path = "/api/admin/jobs")
//@Api(tags = "Admin Operations")
@Slf4j(topic = "graph.feat.admin.ctrl.api")
@SecurityRequirement(name = "api_key")
public class JobsCtrl extends AbstractController {


    private final JobSchedulingService jobsService;


    public JobsCtrl(JobSchedulingService jobsService) {
        this.jobsService = jobsService;

    }


    @PostMapping(value = "/execute/deduplication")
    @ResponseStatus(HttpStatus.OK)
    Mono<Void> execDeduplicationJob() {

        return super.getAuthentication()
                .flatMap(authentication -> this.jobsService.scheduleJob(MergeDuplicatesJob.NAME, authentication))
                .doOnSubscribe(subscription -> log.info("Request to execute job: Deduplication"));
    }

    @PostMapping(value = "/execute/normalize/subjectIdentifiers")
    @ResponseStatus(HttpStatus.ACCEPTED)
    Mono<Void> execReplaceSubjectIdentifiersJob() {

        return super.getAuthentication()
                .flatMap(authentication -> this.jobsService.scheduleJob(ReplaceSubjectIdentifiersJob.NAME, authentication))
                .doOnSubscribe(subscription -> log.info("Request to execute job: Replace subject identifiers"));
    }

    @PostMapping(value = "/execute/normalize/objectIdentifiers")
    @ResponseStatus(HttpStatus.ACCEPTED)
    Mono<Void> execReplaceObjectIdentifiersJob() {
        return super.getAuthentication()
                .flatMap(authentication -> this.jobsService.scheduleJob(ReplaceObjectIdentifiersJob.NAME, authentication))
                .doOnSubscribe(subscription -> log.info("Request to execute job: Replace object identifiers"));

    }


    @PostMapping(value = "/execute/coercion")
    @ResponseStatus(HttpStatus.OK)
    Mono<Void> execCoercionJob() {
        return super.getAuthentication()
                .flatMap(authentication -> this.jobsService.scheduleJob(AssignInternalTypesJob.NAME, authentication))
                .doOnSubscribe(subscription -> log.info("Request to execute job: Infer types"));

    }

    @PostMapping(value = "/execute/export")
    @ResponseStatus(HttpStatus.ACCEPTED)
    Mono<Void> execExportJob() {
        return super.getAuthentication()
                .flatMap(authentication -> this.jobsService.scheduleJob(ExportApplicationJob.NAME, authentication))
                .doOnSubscribe(subscription -> log.info("Request to execute job: Export application"));

    }


}
