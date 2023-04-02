package org.av360.maverick.graph.store.rdf.helpers;

import org.av360.maverick.graph.model.errors.store.DuplicateRecordsException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Helper class to access values in RDF4J's BindingSets
 */
@Slf4j(topic = "graph.repo.queries.util.bindings")
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


    public static Mono<BindingSet> getUniqueBindingSet(List<BindingSet> result) {
        if (result.isEmpty()) return Mono.empty();

        if (result.size() > 1) {
            log.error("Found multiple results when expected exactly one");
            return Mono.error(new DuplicateRecordsException());
        }

        return Mono.just(result.get(0));
    }
}
