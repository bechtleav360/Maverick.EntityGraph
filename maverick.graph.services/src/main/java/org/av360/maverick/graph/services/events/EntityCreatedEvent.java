package org.av360.maverick.graph.services.events;

import org.av360.maverick.graph.store.rdf.models.Transaction;

public class EntityCreatedEvent extends EntityEvent {
    public EntityCreatedEvent(Transaction trx) {
        super(trx);
    }

    @Override
    public String getType() {
        return "maverick.graph.entity.created";
    }

    @Override
    public String getPath() {
        // TODO: find out entity id
        return "api/entities";
    }
}
