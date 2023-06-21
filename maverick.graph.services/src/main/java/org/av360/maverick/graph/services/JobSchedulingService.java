package org.av360.maverick.graph.services;

import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.events.JobScheduledEvent;
import reactor.core.publisher.Mono;

public interface JobSchedulingService {
    Mono<Void> scheduleJob(String name, SessionContext context);

    Mono<Void> publish(JobScheduledEvent event);
}
