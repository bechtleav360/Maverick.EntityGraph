package org.av360.maverick.graph.model.entities;

import org.av360.maverick.graph.model.enums.Activity;
import org.av360.maverick.graph.model.rdf.Triples;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import java.util.Collection;
import java.util.List;

public interface Transaction extends Triples {

    Transaction inserts(Collection<Statement> statements);

    Transaction affects(Collection<Statement> statements);

    Transaction removes(Collection<Statement> statements);

    default Transaction affects(Resource subject, IRI predicate, Value value) {
        return this.affects(List.of(SimpleValueFactory.getInstance().createStatement(subject, predicate, value)));
    }

    default Transaction inserts(Resource subject, IRI predicate, Value value) {
        return this.inserts(subject, predicate, value, null);
    }

    default Transaction inserts(Resource subject, IRI predicate, Value value, Resource context) {
        return this.inserts(List.of(SimpleValueFactory.getInstance().createStatement(subject, predicate, value, context)));
    }

    default Model get(IRI context) {
        return this.get().filter(null, null, null, context);
    }

    Model get();


    IRI getIdentifier();

    void setCompleted();

    void setFailed(String message);


    List<Value> affectedSubjects(Activity ... activities);
}
