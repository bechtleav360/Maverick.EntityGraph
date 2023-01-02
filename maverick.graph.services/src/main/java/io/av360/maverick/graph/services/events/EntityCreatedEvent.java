package io.av360.maverick.graph.services.events;

import io.av360.maverick.graph.store.rdf.models.Transaction;
import org.springframework.context.ApplicationEvent;

public class EntityCreatedEvent extends ApplicationEvent {
    public EntityCreatedEvent(Transaction trx) {
        super(trx);
    }
}
