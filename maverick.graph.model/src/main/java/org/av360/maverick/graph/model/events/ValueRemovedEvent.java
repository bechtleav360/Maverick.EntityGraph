package org.av360.maverick.graph.model.events;

import org.av360.maverick.graph.model.entities.Transaction;

public class ValueRemovedEvent extends EntityEvent {

    public ValueRemovedEvent(Transaction trx) {
        super(trx);
    }

    @Override
    public String getType() {
        return "maverick.graph.value.removed";
    }

    @Override
    public String getPath() {
        // TODO: find out entity id
        return "api/entities";
    }
}
