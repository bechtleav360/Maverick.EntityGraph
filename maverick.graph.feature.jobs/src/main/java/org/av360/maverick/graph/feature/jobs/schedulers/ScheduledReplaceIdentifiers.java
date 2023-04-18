package org.av360.maverick.graph.feature.jobs.schedulers;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.jobs.services.ReplaceExternalIdentifiersService;
import org.av360.maverick.graph.model.security.AdminToken;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

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
public class ScheduledReplaceIdentifiers extends ScheduledJob {

    // FIXME: should not directly access the services
    private final ReplaceExternalIdentifiersService job;


    public ScheduledReplaceIdentifiers(ReplaceExternalIdentifiersService job) {
        this.job = job;
    }


    @Scheduled(initialDelay = 20000, fixedRate = 60000)
    public void checkForGlobalIdentifiersScheduled() {

        AdminToken adminAuthentication = new AdminToken();
        Mono<Void> job = this.job.run(adminAuthentication);
        super.schedule(job, "replace identifiers");
    }

}