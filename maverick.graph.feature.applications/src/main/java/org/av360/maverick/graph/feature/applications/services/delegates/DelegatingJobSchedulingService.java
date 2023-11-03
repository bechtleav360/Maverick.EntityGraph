package org.av360.maverick.graph.feature.applications.services.delegates;

import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.events.JobScheduledEvent;
import org.av360.maverick.graph.model.util.ValidateReactive;
import org.av360.maverick.graph.services.JobSchedulingService;
import reactor.core.publisher.Mono;

public class DelegatingJobSchedulingService implements JobSchedulingService {


    private final JobSchedulingService delegate;

    public DelegatingJobSchedulingService(JobSchedulingService delegate) {
        this.delegate = delegate;
    }

    @Override
    public Mono<Void> scheduleJob(String name, SessionContext context) {

        return ValidateReactive.notNull(context.getEnvironment().getScope())
                .flatMap(label -> {
                            JobScheduledEvent event = new JobScheduledEvent(name, context);
                            return this.publish(event);
                        })
                .switchIfEmpty(delegate.scheduleJob(name, context));
    }

    @Override
    public Mono<Void> publish(JobScheduledEvent event) {
        return delegate.publish(event);
    }
}
