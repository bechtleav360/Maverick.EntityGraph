package org.av360.maverick.graph.feature.jobs.schedulers;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.jobs.ExportRepositoryJob;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.events.JobScheduledEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j(topic = "graph.jobs.exports")
@ConditionalOnProperty(name = "application.features.modules.jobs.scheduled.exportApplication.enabled", havingValue = "true")
public class ScheduledExportApplication {
    private final ApplicationEventPublisher eventPublisher;

    public ScheduledExportApplication(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }


//    @Scheduled(initialDelay = 35, fixedRate = 60, timeUnit = TimeUnit.SECONDS)
    @Scheduled(cron = "${application.features.modules.jobs.scheduled.exportApplication.defaultFrequency:0 */5 * * * ?}")
    public void scheduled() {
        JobScheduledEvent event = new JobScheduledEvent(ExportRepositoryJob.NAME, new SessionContext().setSystemAuthentication());
        eventPublisher.publishEvent(event);
    }
}
