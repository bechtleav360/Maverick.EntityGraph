package io.av360.maverick.graph.services.events;

import org.springframework.context.ApplicationEvent;

public abstract class EntityEvent extends ApplicationEvent {

    public EntityEvent(Object source) {
        super(source);
    }

    public abstract String getType();

    public abstract String getPath();
}
