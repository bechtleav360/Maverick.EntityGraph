package io.av360.maverick.graph.services.events;

import io.av360.maverick.graph.store.rdf.models.Transaction;

public class LinkRemovedEvent extends EntityEvent {

    public LinkRemovedEvent(Transaction trx) {
        super(trx);
    }

    @Override
    public String getType() {
        return "maverick.graph.link.removed";
    }

    @Override
    public String getPath() {
        // TODO: find out entity id
        return "api/entities";
    }
}
