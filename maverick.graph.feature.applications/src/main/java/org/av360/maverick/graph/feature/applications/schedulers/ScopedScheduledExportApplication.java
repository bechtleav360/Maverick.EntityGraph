package org.av360.maverick.graph.feature.applications.schedulers;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.applications.domain.ApplicationsService;
import org.av360.maverick.graph.feature.applications.domain.events.ApplicationJobScheduledEvent;
import org.av360.maverick.graph.model.events.JobScheduledEvent;
import org.av360.maverick.graph.model.security.AdminToken;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j(topic = "graph.jobs.exports")
@ConditionalOnProperty(name = "application.features.modules.jobs.scheduled.exportApplication.enabled", havingValue = "true")
public class ScopedScheduledExportApplication {
    private final ApplicationEventPublisher eventPublisher;
    private final ApplicationsService applicationsService;

    public ScopedScheduledExportApplication(ApplicationEventPublisher eventPublisher, ApplicationsService applicationsService) {
        this.eventPublisher = eventPublisher;
        this.applicationsService = applicationsService;
    }

    @Scheduled(initialDelay = 10, fixedRate = 60, timeUnit = TimeUnit.SECONDS)
    public void scheduled() {
        applicationsService.listApplications(new AdminToken())
                .doOnNext(application -> {
                    JobScheduledEvent event = new ApplicationJobScheduledEvent("exportApplication", new AdminToken(), application);
                    eventPublisher.publishEvent(event);
                }).subscribe();
    }
}
