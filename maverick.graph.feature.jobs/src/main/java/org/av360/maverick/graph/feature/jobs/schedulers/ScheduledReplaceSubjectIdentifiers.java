package org.av360.maverick.graph.feature.jobs.schedulers;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.jobs.ReplaceLinkedIdentifiersJob;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.events.JobScheduledEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * After the object identifiers have been replaced, this job is responsible to replace also the objects pointing to the
 * old object identifiers with the new identifiers. The latter are stored in a property.
 */
@Slf4j(topic = "graph.jobs.identifiers")
@Component
@ConditionalOnProperty(name = "application.features.modules.jobs.scheduled.replaceIdentifiers.enabled", havingValue = "true")
public class ScheduledReplaceSubjectIdentifiers {

    // FIXME: should not directly access the services
    private final ApplicationEventPublisher eventPublisher;

    public ScheduledReplaceSubjectIdentifiers(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }


//    @Scheduled(initialDelay = 120, fixedRate = 600, timeUnit = TimeUnit.SECONDS)
    @Scheduled(cron = "${application.features.modules.jobs.scheduled.replaceIdentifiers.defaultFrequency:0 */5 * * * ?}")
    public void checkForGlobalIdentifiersScheduled() {
        JobScheduledEvent event = new JobScheduledEvent(ReplaceLinkedIdentifiersJob.NAME, new SessionContext().setSystemAuthentication());
        eventPublisher.publishEvent(event);
    }

}