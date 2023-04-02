package org.av360.maverick.graph.feature.applications.domain.events;

import org.springframework.context.ApplicationEvent;

public class ApplicationDeletedEvent extends ApplicationEvent {

    final String label;

    public ApplicationDeletedEvent(String label) {
        super(label);
        this.label = label;
    }

    public String getApplicationLabel() {
        return this.label;
    }
}
