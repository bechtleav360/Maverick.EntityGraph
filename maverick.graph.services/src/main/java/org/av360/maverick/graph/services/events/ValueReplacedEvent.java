package org.av360.maverick.graph.services.events;

import org.av360.maverick.graph.store.rdf.models.Transaction;

public class ValueReplacedEvent extends EntityEvent {

    public ValueReplacedEvent(Transaction trx) {
        super(trx);
    }

    @Override
    public String getType() {
        return "maverick.graph.value.replaced";
    }

    @Override
    public String getPath() {
        // TODO: find out entity id
        return "api/entities";
    }
}
