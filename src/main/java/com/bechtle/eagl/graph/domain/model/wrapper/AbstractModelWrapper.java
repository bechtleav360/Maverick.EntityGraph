package com.bechtle.eagl.graph.domain.model.wrapper;

import com.bechtle.eagl.graph.domain.model.extensions.NamespaceAwareStatement;
import com.bechtle.eagl.graph.domain.model.extensions.NamespacedModelBuilder;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.ModelBuilder;

import java.io.Serializable;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class AbstractModelWrapper<T> implements NamespaceAware, Serializable {

    private ModelBuilder modelBuilder;

    protected AbstractModelWrapper() {
        this.modelBuilder = new NamespacedModelBuilder();
    }


    @Override
    public Set<Namespace> getNamespaces() {
        return this.getModel().getNamespaces();
    }

    public ModelBuilder getBuilder() {
        return modelBuilder;
    }

    public Model getModel() {
        return getBuilder().build();
    }

    protected void reset() {
        this.modelBuilder = new NamespacedModelBuilder();
    }

    public Stream<NamespaceAwareStatement> stream() {
        return this.getModel().stream().map(sts -> NamespaceAwareStatement.wrap(sts, getNamespaces()));
    }

    public Iterable<? extends NamespaceAwareStatement> asStatements() {
        return this.stream().toList();
    }

    public Stream<Value> streamValues(Resource subject, IRI predicate) {
        Iterable<Statement> statements = this.getModel().getStatements(subject, predicate, null);
        return StreamSupport.stream(statements.spliterator(), true).map(statement -> statement.getObject());
    }

    @Override
    public String toString() {
        return this.getModel().toString();
    }


}
