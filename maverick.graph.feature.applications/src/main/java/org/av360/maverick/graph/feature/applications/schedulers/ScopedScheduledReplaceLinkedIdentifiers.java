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
public class ScopedScheduledReplaceLinkedIdentifiers {

    // FIXME: should not directly access the services
    private final ApplicationEventPublisher eventPublisher;

    private final ApplicationsService applicationsService;

    public ScopedScheduledReplaceLinkedIdentifiers(ApplicationEventPublisher eventPublisher, ApplicationsService applicationsService) {
        this.eventPublisher = eventPublisher;
        this.applicationsService = applicationsService;
    }


    // @Scheduled(initialDelay = 150, fixedRate = 600, timeUnit = TimeUnit.SECONDS)
    @Scheduled(initialDelay = 350, fixedRate = 600, timeUnit = TimeUnit.SECONDS)
    public void checkForGlobalIdentifiersScheduled() {


        applicationsService.listApplications(new AdminToken())
                .doOnNext(application -> {
                    JobScheduledEvent event = new ApplicationJobScheduledEvent("replaceLinkedIdentifiers", new AdminToken(), application);
                    eventPublisher.publishEvent(event);
                }).subscribe();

    }

}