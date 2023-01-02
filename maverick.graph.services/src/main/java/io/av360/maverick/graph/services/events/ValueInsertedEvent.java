package io.av360.maverick.graph.services.events;

import io.av360.maverick.graph.store.rdf.models.Transaction;
import org.springframework.context.ApplicationEvent;

public class ValueInsertedEvent extends ApplicationEvent {

    public ValueInsertedEvent(Transaction trx) {
        super(trx);
    }
}
