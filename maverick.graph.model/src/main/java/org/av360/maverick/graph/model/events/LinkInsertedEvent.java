package org.av360.maverick.graph.model.events;

import org.av360.maverick.graph.model.entities.Transaction;

public class LinkInsertedEvent extends EntityEvent {

    public LinkInsertedEvent(Transaction trx) {
        super(trx);
    }

    @Override
    public String getType() {
        return "maverick.graph.link.inserted";
    }

    @Override
    public String getPath() {
        // TODO: find out entity id
        return "api/entities";
    }
}
