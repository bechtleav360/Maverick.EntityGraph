package org.av360.maverick.graph.model.rdf;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * see standard implementation in rdf4j instead (with the same name, argh)
 *
 * We keep this for future reference
 */
@Deprecated
public class UnnecessaryModelCollector implements Collector<Statement, Model, Model> {
    @Override
    public Supplier<Model> supplier() {
        return LinkedHashModel::new;
    }

    @Override
    public BiConsumer<Model, Statement> accumulator() {
        return (model, statement) -> model.add(statement.getSubject(), statement.getPredicate(), statement.getObject());
    }

    @Override
    public BinaryOperator<Model> combiner() {
        return (m1, m2) -> {
            m1.addAll(m2);
            return m1;
        };
    }

    @Override
    public Function<Model, Model> finisher() {
        return Model::unmodifiable;
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Set.of(Characteristics.UNORDERED);
    }
}
