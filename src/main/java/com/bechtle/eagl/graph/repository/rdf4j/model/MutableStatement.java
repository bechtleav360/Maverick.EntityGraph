package com.bechtle.eagl.graph.repository.rdf4j.model;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleStatement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public class MutableStatement extends SimpleStatement {


    public MutableStatement(Resource subject, IRI predicate, Value object) {
        super(subject, predicate, object);
    }
}
