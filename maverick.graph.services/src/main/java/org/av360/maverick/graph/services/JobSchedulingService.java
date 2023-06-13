package org.av360.maverick.graph.services;

import org.av360.maverick.graph.model.events.JobScheduledEvent;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

public interface JobSchedulingService {
    Mono<Void> scheduleJob(String name, Authentication authentication);

    Mono<Void> publish(JobScheduledEvent event);
}
