package org.av360.maverick.graph.model.events;

import org.av360.maverick.graph.model.entities.Transaction;

public class DetailInsertedEvent extends EntityEvent {

    public DetailInsertedEvent(Transaction trx) {
        super(trx);
    }

    @Override
    public String getType() {
        return "maverick.graph.detail.inserted";
    }

    @Override
    public String getPath() {
        // TODO: find out entity id
        return "api/entities";
    }
}
