package io.av360.maverick.graph.services.events;

import io.av360.maverick.graph.store.rdf.models.Transaction;
import org.springframework.context.ApplicationEvent;

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
