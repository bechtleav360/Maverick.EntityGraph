package org.av360.maverick.graph.model.events;

import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.entities.Transaction;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.util.Set;

public class EntityCreatedEvent extends EntityEvent {
    public EntityCreatedEvent(Transaction trx, Environment environment) {
        super(trx, environment);
    }

    @Override
    public String getType() {
        return "maverick.graph.entity.created";
    }


    public Set<Resource> listInsertedFragmentSubjects() {
        return super.getTransaction().getInsertedStatements().filter(null, RDF.TYPE, null).subjects();
    }

    @Override
    public String getPath() {
        // TODO: find out entity id
        return "api/entities";
    }
}
