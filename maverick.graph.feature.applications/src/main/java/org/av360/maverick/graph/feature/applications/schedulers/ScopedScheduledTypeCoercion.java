package org.av360.maverick.graph.feature.applications.schedulers;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.applications.domain.ApplicationsService;
import org.av360.maverick.graph.feature.applications.events.ApplicationJobScheduledEvent;
import org.av360.maverick.graph.model.events.JobScheduledEvent;
import org.av360.maverick.graph.model.security.AdminToken;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @see Sch
 */
@Slf4j(topic = "graph.jobs.identifiers")
@Component
@ConditionalOnProperty(name = "application.features.modules.jobs.scheduled.typeCoercion", havingValue = "true")
public class ScopedScheduledTypeCoercion {

    private final ApplicationEventPublisher eventPublisher;
    private final ApplicationsService applicationsService;
    public ScopedScheduledTypeCoercion(ApplicationEventPublisher eventPublisher, ApplicationsService applicationsService) {
        this.eventPublisher = eventPublisher;
        this.applicationsService = applicationsService;
    }

    @Scheduled(initialDelay = 10000, fixedDelay = 30000)
    public void scheduled() {
        applicationsService.listApplications(new AdminToken())
                .doOnNext(application -> {
                    JobScheduledEvent event = new ApplicationJobScheduledEvent("typeCoercion", new AdminToken(), application);
                    eventPublisher.publishEvent(event);
                }).subscribe();
    }

}