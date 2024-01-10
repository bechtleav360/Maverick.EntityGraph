package org.av360.maverick.graph.feature.jobs.schedulers;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.jobs.jobs.ReplaceLinkedIdentifiersJob;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.events.JobScheduledEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * If we have any global identifiers (externally set) or anonymous nodes in the repo, we have to replace them
 * with our internal identifiers. Otherwise, we cannot address the entities through our API.
 *
 * @see ReplaceLinkedIdentifiersJob
 */
@Slf4j(topic = "graph.jobs.identifiers")
@Component
@ConditionalOnProperty(name = "application.features.modules.jobs.scheduled.replaceIdentifiers.enabled", havingValue = "true")
public class ScheduledReplaceObjectIdentifiers {

    // FIXME: should not directly access the services
    private final ApplicationEventPublisher eventPublisher;

    public ScheduledReplaceObjectIdentifiers(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }


//    @Scheduled(initialDelay = 90, fixedRate = 600, timeUnit = TimeUnit.SECONDS)
    @Scheduled(cron = "${application.features.modules.jobs.scheduled.replaceIdentifiers.defaultFrequency:0 */5 * * * ?}")
    public void checkForGlobalIdentifiersScheduled() {
        JobScheduledEvent event = new JobScheduledEvent(ReplaceLinkedIdentifiersJob.NAME, new SessionContext().setSystemAuthentication());
        eventPublisher.publishEvent(event);
    }

}