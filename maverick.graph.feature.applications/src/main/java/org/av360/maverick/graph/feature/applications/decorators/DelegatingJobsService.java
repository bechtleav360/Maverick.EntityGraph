package org.av360.maverick.graph.feature.applications.decorators;

import org.av360.maverick.graph.feature.applications.config.ReactiveApplicationContextHolder;
import org.av360.maverick.graph.feature.applications.services.events.ApplicationJobScheduledEvent;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.events.JobScheduledEvent;
import org.av360.maverick.graph.services.JobSchedulingService;
import reactor.core.publisher.Mono;

public class DelegatingJobsService implements JobSchedulingService {


    private final JobSchedulingService delegate;

    public DelegatingJobsService(JobSchedulingService delegate) {
        this.delegate = delegate;
    }

    @Override
    public Mono<Void> scheduleJob(String name, SessionContext context) {
        return ReactiveApplicationContextHolder.getRequestedApplicationLabel()
                .flatMap(applicationLabel -> {
                    context.withEnvironment().setScope(applicationLabel);

                    JobScheduledEvent event = new ApplicationJobScheduledEvent(name, context, applicationLabel);
                    return this.publish(event);
                })
                .switchIfEmpty(delegate.scheduleJob(name, context));
    }

    @Override
    public Mono<Void> publish(JobScheduledEvent event) {
        return delegate.publish(event);
    }
}
