package org.av360.maverick.graph.services.events;

import org.av360.maverick.graph.store.rdf.models.Transaction;

public class EntityDeletedEvent extends EntityEvent {

    public EntityDeletedEvent(Transaction trx) {
        super(trx);
    }

    @Override
    public String getType() {
        return "maverick.graph.entity.deleted";
    }

    @Override
    public String getPath() {
        // TODO: find out entity id
        return "api/entities";
    }
}
