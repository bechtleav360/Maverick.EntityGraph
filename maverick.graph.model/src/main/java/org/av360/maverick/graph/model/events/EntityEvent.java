package org.av360.maverick.graph.model.events;

import org.av360.maverick.graph.model.entities.Transaction;
import org.springframework.context.ApplicationEvent;

public abstract class EntityEvent extends ApplicationEvent {

    public EntityEvent(Transaction source) {
        super(source);
    }

    public abstract String getType();

    public Transaction getTransaction() {
        return (Transaction) super.getSource();
    }

    public abstract String getPath();
}
