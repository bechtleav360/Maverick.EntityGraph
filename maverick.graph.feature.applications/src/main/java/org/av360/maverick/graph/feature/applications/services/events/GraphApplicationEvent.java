package org.av360.maverick.graph.feature.applications.services.events;

import org.springframework.context.ApplicationEvent;

public abstract class GraphApplicationEvent<T> extends ApplicationEvent {

    public GraphApplicationEvent(T source) {
        super(source);
    }

    public abstract String getLabel();
}
