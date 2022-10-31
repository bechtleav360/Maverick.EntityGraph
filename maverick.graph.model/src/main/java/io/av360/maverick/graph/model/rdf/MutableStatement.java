package io.av360.maverick.graph.model.rdf;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleStatement;

public class MutableStatement extends SimpleStatement {


    public MutableStatement(Resource subject, IRI predicate, Value object) {
        super(subject, predicate, object);
    }
}
