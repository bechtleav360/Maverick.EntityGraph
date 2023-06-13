package org.av360.maverick.graph.feature.jobs.services;

import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.events.JobScheduledEvent;
import org.av360.maverick.graph.services.JobSchedulingService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class DefaultJobsService implements JobSchedulingService {

    private final ApplicationEventPublisher eventPublisher;

    public DefaultJobsService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Mono<Void> scheduleJob(String name, SessionContext ctx) {
        JobScheduledEvent event = new JobScheduledEvent(name, ctx);
        return this.publish(event);

    }

    @Override
    public Mono<Void> publish(JobScheduledEvent event) {
        eventPublisher.publishEvent(event);
        return Mono.empty();
    }
}
