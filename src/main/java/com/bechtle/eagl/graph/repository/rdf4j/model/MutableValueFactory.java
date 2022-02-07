package com.bechtle.eagl.graph.repository.rdf4j.model;

import org.eclipse.rdf4j.model.*;
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
