package org.av360.maverick.graph.feature.applications.domain.events;

import org.av360.maverick.graph.feature.applications.config.ReactiveApplicationContextHolder;
import org.av360.maverick.graph.model.events.JobScheduledEvent;
import org.springframework.security.core.Authentication;
import reactor.util.context.Context;

public class ApplicationJobScheduledEvent extends JobScheduledEvent {
    private final String requestedApplicationLabel;

    public ApplicationJobScheduledEvent(String label, Authentication authentication, String requestedApplicationLabel) {
        super(label, authentication);
        this.requestedApplicationLabel = requestedApplicationLabel;
    }

    @Override
    public Context buildContext(Context ctx) {
        ctx = ctx.putAll(ReactiveApplicationContextHolder.withApplicationLabel(requestedApplicationLabel).readOnly());
        return super.buildContext(ctx);
    }

    @Override
    public String getScope() {
        return requestedApplicationLabel;
    }

}
