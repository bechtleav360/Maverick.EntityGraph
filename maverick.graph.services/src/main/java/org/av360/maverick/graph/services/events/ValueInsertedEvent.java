package org.av360.maverick.graph.services.events;

import org.av360.maverick.graph.store.rdf.models.Transaction;

public class ValueInsertedEvent extends EntityEvent {

    public ValueInsertedEvent(Transaction trx) {
        super(trx);
    }

    @Override
    public String getType() {
        return "maverick.graph.value.inserted";
    }

    @Override
    public String getPath() {
        // TODO: find out entity id
        return "api/entities";
    }
}
