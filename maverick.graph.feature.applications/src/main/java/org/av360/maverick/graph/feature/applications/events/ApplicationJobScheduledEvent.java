package org.av360.maverick.graph.feature.applications.events;

import org.av360.maverick.graph.feature.applications.config.ReactiveApplicationContextHolder;
import org.av360.maverick.graph.feature.applications.domain.model.Application;
import org.av360.maverick.graph.model.events.JobScheduledEvent;
import org.springframework.security.core.Authentication;
import reactor.util.context.Context;

public class ApplicationJobScheduledEvent extends JobScheduledEvent {
    private final Application requestedApplication;

    public ApplicationJobScheduledEvent(String label, Authentication authentication, Application requestedApplication) {
        super(label, authentication);
        this.requestedApplication = requestedApplication;
    }

    @Override
    public Context buildContext(Context ctx) {
        ctx = ctx.putAll(ReactiveApplicationContextHolder.withApplication(this.requestedApplication).readOnly());
        return super.buildContext(ctx);
    }

    @Override
    public String getJobIdentifier() {
        return String.format("%s (%s)", super.getJobName(), this.requestedApplication.label());
    }
}
