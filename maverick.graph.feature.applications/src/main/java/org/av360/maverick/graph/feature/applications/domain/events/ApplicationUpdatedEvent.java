package org.av360.maverick.graph.feature.applications.domain.events;

import org.springframework.context.ApplicationEvent;

public abstract class ApplicationUpdatedEvent<T> extends ApplicationEvent {

    public ApplicationUpdatedEvent(T source) {
        super(source);
    }

    public abstract String getLabel();
}
