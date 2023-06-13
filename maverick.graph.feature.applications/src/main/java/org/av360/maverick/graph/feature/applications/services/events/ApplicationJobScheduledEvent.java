package org.av360.maverick.graph.feature.applications.services.events;

import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.events.JobScheduledEvent;

public class ApplicationJobScheduledEvent extends JobScheduledEvent {
    private final String requestedApplicationLabel;

    public ApplicationJobScheduledEvent(String label, SessionContext context, String requestedApplicationLabel) {
        super(label, context);
        this.requestedApplicationLabel = requestedApplicationLabel;
    }


    @Override
    public String getScope() {
        return requestedApplicationLabel;
    }

}
