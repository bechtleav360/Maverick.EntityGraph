package org.av360.maverick.graph.feature.jobs.ctrl;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.api.controller.AbstractController;
import org.av360.maverick.graph.feature.jobs.DetectDuplicatesJob;
import org.av360.maverick.graph.feature.jobs.ReplaceExternalIdentifiersJob;
import org.av360.maverick.graph.feature.jobs.ReplaceLinkedExternalIdentifiersJob;
import org.av360.maverick.graph.feature.jobs.TypeCoercionJob;
import org.av360.maverick.graph.model.events.JobScheduledEvent;
import org.av360.maverick.graph.model.security.AdminToken;
import org.springframework.context.ApplicationEventPublisher;
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


    private final ApplicationEventPublisher eventPublisher;


    public JobsCtrl(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;

    }


    @PostMapping(value = "/execute/deduplication")
    @ResponseStatus(HttpStatus.OK)
    Mono<Void> execDeduplicationJob() {
        log.info("Request to execute job: Deduplication");
        JobScheduledEvent event = new JobScheduledEvent(DetectDuplicatesJob.NAME, new AdminToken());
        eventPublisher.publishEvent(event);
        return Mono.empty();

    }

    @PostMapping(value = "/execute/normalize/subjectIdentifiers")
    @ResponseStatus(HttpStatus.ACCEPTED)
    Mono<Void> execReplaceSubjectIdentifiersJob() {
        log.info("Request to execute job: Replace subject identifiers");
        JobScheduledEvent event = new JobScheduledEvent(ReplaceExternalIdentifiersJob.NAME, new AdminToken());
        eventPublisher.publishEvent(event);
        return Mono.empty();
    }

    @PostMapping(value = "/execute/normalize/objectIdentifiers")
    @ResponseStatus(HttpStatus.ACCEPTED)
    Mono<Void> execReplaceObjectIdentifiersJob() {
        log.info("Request to execute job: Replace object identifiers");
        JobScheduledEvent event = new JobScheduledEvent(ReplaceLinkedExternalIdentifiersJob.NAME, new AdminToken());
        eventPublisher.publishEvent(event);
        return Mono.empty();

    }


    @PostMapping(value = "/execute/coercion")
    @ResponseStatus(HttpStatus.OK)
    Mono<Void> execCoercionJob() {
        log.info("Request to execute job: Infer types");
        JobScheduledEvent event = new JobScheduledEvent(TypeCoercionJob.NAME, new AdminToken());
        eventPublisher.publishEvent(event);
        return Mono.empty();
    }


}
