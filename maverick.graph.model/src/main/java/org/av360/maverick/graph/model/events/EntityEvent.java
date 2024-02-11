package org.av360.maverick.graph.model.events;

import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.entities.Transaction;
import org.springframework.context.ApplicationEvent;

public abstract class EntityEvent extends ApplicationEvent {

    private final Environment environment;

    public EntityEvent(Transaction source, Environment environment) {
        super(source);
        this.environment = environment;
    }

    public abstract String getType();

    public Transaction getTransaction() {
        return (Transaction) super.getSource();
    }

    public abstract String getPath();

    public Environment getEnvironment() {
        return environment;
    }
}
