package io.av360.maverick.graph.model.rdf;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.springframework.stereotype.Service;

@Service
public class MutableValueFactory extends SimpleValueFactory {

    @Override
    public Statement createStatement(Resource subject, IRI predicate, Value object) {
        return new MutableStatement(subject, predicate, object);
    }

    @Override
    public Statement createStatement(Resource subject, IRI predicate, Value object, Resource context) {
        return super.createStatement(subject,predicate,object,context);
    }

}
