package com.bechtle.eagl.graph.repository.rdf4j.extensions;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;

public class BindingsAccessor {
    private final BindingSet bindings;

    public BindingsAccessor(BindingSet bindings) {
        this.bindings = bindings;
    }

    public IRI asIRI(Variable var) {
        return (IRI) this.bindings.getValue(var.getVarName());
    }

    public String asString(Variable var) {
        return this.bindings.getValue(var.getVarName()).stringValue();
    }

    public boolean asBoolean(Variable var) {
        return Literals.getBooleanValue(bindings.getValue(var.getVarName()), false);
    }
}
