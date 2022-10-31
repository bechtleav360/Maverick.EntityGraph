package io.av360.maverick.graph.store.rdf.helpers;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.query.BindingSet;  //FIXME
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;

/**
 * Helper class to access values in RDF4J's BindingSets
 */
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
