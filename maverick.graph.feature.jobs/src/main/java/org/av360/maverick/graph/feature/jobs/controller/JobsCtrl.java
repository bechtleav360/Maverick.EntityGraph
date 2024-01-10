package org.av360.maverick.graph.feature.jobs.controller;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.api.controller.AbstractController;
import org.av360.maverick.graph.feature.jobs.jobs.*;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.services.JobSchedulingService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController

@Slf4j(topic = "graph.feat.admin.ctrl.api")
public class JobsCtrl extends AbstractController implements org.av360.maverick.graph.feature.jobs.JobsWebApi {

    private final JobSchedulingService jobsService;


    public JobsCtrl(JobSchedulingService jobsService) {
        this.jobsService = jobsService;

    }


    @Override
    public Mono<Void> execDeduplicationJob() {

        return super.acquireContext()
                .flatMap(ctx -> this.jobsService.scheduleJob(MergeDuplicatesJob.NAME, ctx))
                .doOnSubscribe(subscription -> log.info("Request to execute job: Deduplication"));
    }

    @Override
    @PostMapping(value = "/execute/normalize/subjectIdentifiers")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Void> execReplaceSubjectIdentifiersJob() {

        return super.acquireContext()
                .flatMap(ctx -> this.jobsService.scheduleJob(ReplaceSubjectIdentifiersJob.NAME, ctx))
                .doOnSubscribe(subscription -> log.info("Request to execute job: Replace subject identifiers"));
    }

    @Override
    @PostMapping(value = "/execute/normalize/objectIdentifiers")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Void> execReplaceObjectIdentifiersJob() {
        return super.acquireContext()
                .flatMap(ctx -> this.jobsService.scheduleJob(ReplaceLinkedIdentifiersJob.NAME, ctx))
                .doOnSubscribe(subscription -> log.info("Request to execute job: Replace object identifiers"));

    }


    @Override
    @PostMapping(value = "/execute/coercion")
    @ResponseStatus(HttpStatus.OK)
    public Mono<Void> execCoercionJob() {
        return super.acquireContext()
                .flatMap(ctx -> this.jobsService.scheduleJob(AssignInternalTypesJob.NAME, ctx))
                .doOnSubscribe(subscription -> log.info("Request to execute job: Infer types"));

    }

    @Override
    @PostMapping(value = "/execute/export")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Void> execExportJob(@RequestParam(required = false, defaultValue = "entities", value = "entities") @Parameter(name = "repository", description = "The repository type in which the query should search.")
                                    RepositoryType repositoryType) {
        return super.acquireContext()
                .map(context -> context.getEnvironment().withRepositoryType(repositoryType))
                .flatMap(ctx -> this.jobsService.scheduleJob(ExportRepositoryJob.NAME, ctx))
                .doOnSubscribe(subscription -> log.info("Request to execute job: Export application"));

    }


}
