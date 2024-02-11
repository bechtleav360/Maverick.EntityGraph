package org.av360.maverick.graph.model.events;

import org.av360.maverick.graph.model.entities.Transaction;
import org.eclipse.rdf4j.model.IRI;

import java.util.Set;

public class EntityCreatedEvent extends EntityEvent {
    public EntityCreatedEvent(Transaction trx) {
        super(trx);
    }

    @Override
    public String getType() {
        return "maverick.graph.entity.created";
    }


    public Set<IRI> listCreatedEntityIdentifiers() {
        return Set.of();
    }

    @Override
    public String getPath() {
        // TODO: find out entity id
        return "api/entities";
    }
}
