package org.av360.maverick.graph.feature.jobs.domain;

import org.av360.maverick.graph.model.events.JobScheduledEvent;
import org.av360.maverick.graph.services.JobSchedulingService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class DefaultJobsService implements JobSchedulingService {

    private final ApplicationEventPublisher eventPublisher;

    public DefaultJobsService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Mono<Void> scheduleJob(String name, Authentication authentication) {
        JobScheduledEvent event = new JobScheduledEvent(name, authentication);
        return this.publish(event);

    }

    @Override
    public Mono<Void> publish(JobScheduledEvent event) {
        eventPublisher.publishEvent(event);
        return Mono.empty();
    }
}
