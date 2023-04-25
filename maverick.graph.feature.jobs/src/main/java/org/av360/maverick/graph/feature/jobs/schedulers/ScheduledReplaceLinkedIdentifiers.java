package org.av360.maverick.graph.feature.jobs.schedulers;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.jobs.ReplaceLinkedExternalIdentifiersJob;
import org.av360.maverick.graph.model.events.JobScheduledEvent;
import org.av360.maverick.graph.model.security.AdminToken;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * If we have any global identifiers (externally set) in the repo, we have to replace them with our internal identifiers.
 * Otherwise we cannot address the entities through our API.
 * <p>
 * Periodically runs the following sparql queries, grabs the entity definition for it and regenerates the identifiers
 * <p>
 * SELECT ?a WHERE { ?a a ?c . }
 * FILTER NOT EXISTS {
 * FILTER STRSTARTS(str(?a), "http://graphs.azurewebsites.net/api/entities/").
 * }
 * LIMIT 100
 */
@Slf4j(topic = "graph.jobs.identifiers")
@Component
@ConditionalOnProperty(name = "application.features.modules.jobs.scheduled.replaceIdentifiers", havingValue = "true")
public class ScheduledReplaceLinkedIdentifiers {

    // FIXME: should not directly access the services
    private final ApplicationEventPublisher eventPublisher;

    public ScheduledReplaceLinkedIdentifiers(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }


    @Scheduled(initialDelay = 120, fixedRate = 600, timeUnit = TimeUnit.SECONDS)
    // @Scheduled(initialDelay = 7, fixedRate = 20, timeUnit = TimeUnit.SECONDS)
    public void checkForGlobalIdentifiersScheduled() {
        JobScheduledEvent event = new JobScheduledEvent(ReplaceLinkedExternalIdentifiersJob.NAME, new AdminToken());
        eventPublisher.publishEvent(event);
    }

}