package io.av360.maverick.graph.services.events;

import io.av360.maverick.graph.store.rdf.models.Transaction;
import org.springframework.context.ApplicationEvent;

public class EntityDeletedEvent extends ApplicationEvent {

    public EntityDeletedEvent(Transaction trx) {
        super(trx);
    }
}
