package io.av360.maverick.graph.services.events;

import io.av360.maverick.graph.store.rdf.models.Transaction;
import org.springframework.context.ApplicationEvent;

public class ValueReplacedEvent extends ApplicationEvent {

    public ValueReplacedEvent(Transaction trx) {
        super(trx);
    }
}
