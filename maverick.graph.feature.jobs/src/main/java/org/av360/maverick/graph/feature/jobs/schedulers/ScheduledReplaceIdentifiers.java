package org.av360.maverick.graph.feature.jobs.schedulers;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.jobs.services.ReplaceExternalIdentifiersService;
import org.av360.maverick.graph.model.security.AdminToken;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
public class ScheduledReplaceIdentifiers {

    // FIXME: should not directly access the services
    private final ReplaceExternalIdentifiersService job;


    public ScheduledReplaceIdentifiers(ReplaceExternalIdentifiersService job) {
        this.job = job;
    }


    @Scheduled(fixedDelay = 30000)
    public void checkForGlobalIdentifiersScheduled() {
        if (this.job.isRunning()) return;

        AdminToken adminAuthentication = new AdminToken();
        this.job.run(adminAuthentication).subscribe();


        /*
        this.job.checkForExternalIdentifiers(adminAuthentication)
                .collectList()
                .doOnError(throwable -> log.error("Checking for global identifiers failed. ", throwable))
                .doOnSuccess(list -> {
                    Integer reduce = list.stream()
                            .map(transaction -> transaction.listModifiedResources().size())
                            .reduce(0, Integer::sum);
                    if (reduce > 0) {
                        log.info("Checking for external identifiers completed, {} resource identifiers have been converted to locally resolvable identifiers.", reduce);
                    } else {
                        log.trace("No global identifiers found");
                    }

                }).subscribe();
                */
    }

}