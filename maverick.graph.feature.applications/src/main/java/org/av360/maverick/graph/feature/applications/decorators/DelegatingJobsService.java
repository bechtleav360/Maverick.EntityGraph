package org.av360.maverick.graph.feature.applications.decorators;

import org.av360.maverick.graph.feature.applications.config.ReactiveApplicationContextHolder;
import org.av360.maverick.graph.feature.applications.services.events.ApplicationJobScheduledEvent;
import org.av360.maverick.graph.model.events.JobScheduledEvent;
import org.av360.maverick.graph.services.JobSchedulingService;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

public class DelegatingJobsService implements JobSchedulingService {


    private final JobSchedulingService delegate;

    public DelegatingJobsService(JobSchedulingService delegate) {
        this.delegate = delegate;
    }

    @Override
    public Mono<Void> scheduleJob(String name, Authentication authentication) {
        return ReactiveApplicationContextHolder.getRequestedApplicationLabel()
                .flatMap(applicationLabel -> {
                    JobScheduledEvent event = new ApplicationJobScheduledEvent(name, authentication, applicationLabel);
                    return this.publish(event);
                })
                .switchIfEmpty(delegate.scheduleJob(name, authentication));
    }

    @Override
    public Mono<Void> publish(JobScheduledEvent event) {
        return delegate.publish(event);
    }
}
