package org.av360.maverick.graph.feature.jobs.schedulers;


import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.jobs.jobs.MergeDuplicateEmbedsJobs;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.events.JobScheduledEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Regular check for duplicate embedded values
 *
 * A duplicate embedded is a nested value, where all (nested) properties are the same.
 */
@Component
@Slf4j(topic = "graph.jobs.duplicateEmbeddeds")
@ConditionalOnProperty(name = "application.features.modules.jobs.scheduled.detectDuplicateEmbeddeds.enabled", havingValue = "true")
public class ScheduledDetectDuplicateEmbeds {

    private final ApplicationEventPublisher eventPublisher;

    public ScheduledDetectDuplicateEmbeds(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }


    // https://github.com/spring-projects/spring-framework/issues/23533

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
    public void checkForDuplicateEmbedsScheduled() {
        JobScheduledEvent event = new JobScheduledEvent(MergeDuplicateEmbedsJobs.NAME, new SessionContext().setSystemAuthentication());
        eventPublisher.publishEvent(event);

    }
}