package org.av360.maverick.graph.store.rdf.helpers;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.errors.InconsistentModelException;
import org.av360.maverick.graph.model.errors.store.DuplicateRecordsException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Helper class to access values in RDF4J's BindingSets
 */
@Slf4j(topic = "graph.repo.queries.util.bindings")
public class BindingsAccessor {
    private final BindingSet bindings;

    public BindingsAccessor(BindingSet bindings) {
        this.bindings = bindings;
    }

    public IRI asIRI(Variable var) throws InconsistentModelException {
        return this.asIRI(var.getVarName());
    }

    public IRI asIRI(String var) throws InconsistentModelException {
        Value value = this.bindings.getValue(var);

        if(Objects.isNull(value)) throw new InconsistentModelException("Trying to access missing binding '%s' in query result".formatted(var));
        if(! value.isIRI())  throw new InconsistentModelException("Trying to access binding '%s' as IRI in query result, but it is of type %s".formatted(var, value.getClass()));

        return (IRI) value;
    }

    public Resource asResource(String var) throws InconsistentModelException {
        Value value = this.bindings.getValue(var);

        if(Objects.isNull(value)) throw new InconsistentModelException("Trying to access missing binding '%s' in query result".formatted(var));
        if(! value.isResource())  throw new InconsistentModelException("Trying to access binding '%s' as resource in query result, but it is of type %s".formatted(var, value.getClass()));

        return (Resource) value;
    }

    public String asString(Variable var) {
        return this.asString(var.getVarName());
    }
    public String asString(String var) {
        return this.findString(var).orElseThrow();
    }

    public Optional<String> findString(String var) {
        return findValue(var).map(Value::stringValue);
    }


    public Set<String> asSet(Variable var, String separator) {
        return this.asSet(var.getVarName(), separator);
    }

    public Set<String> asSet(String concatenatedStringVarName, String separator) {
        return this.findValue(concatenatedStringVarName)
                .map(Value::stringValue)
                .map(s -> s.split(separator))
                .map(Set::of)
                .orElse(Set.of());
    }

    public Optional<Value> findValue(String var) {
        return Optional.ofNullable(this.bindings.getValue(var));
    }

    public Optional<Value> findValue(Variable var) {
        return this.findValue(var.getVarName());
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
