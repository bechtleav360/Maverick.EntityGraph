package org.av360.maverick.graph.feature.jobs.schedulers;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.jobs.services.TypeCoercionService;
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
@ConditionalOnProperty(name = "application.features.modules.jobs.scheduled.typeCoercion", havingValue = "true")
public class ScheduledTypeCoercion {

    // FIXME: should not directly access the services
    private final TypeCoercionService job;


    public ScheduledTypeCoercion(TypeCoercionService job) {
        this.job = job;
    }


    @Scheduled(fixedDelay = 20000)
    public void scheduled() {
        if (this.job.isRunning()) return;

        AdminToken adminAuthentication = new AdminToken();

        this.job.run(adminAuthentication).subscribe();
    }

}