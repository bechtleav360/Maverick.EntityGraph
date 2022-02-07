package com.bechtle.eagl.graph.model.wrapper;

import com.bechtle.eagl.graph.model.GeneratedIdentifier;
import com.bechtle.eagl.graph.model.NamespaceAwareStatement;
import com.bechtle.eagl.graph.model.vocabulary.Transactions;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryResult;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class AbstractModelWrapper<T> implements NamespaceAware, Serializable {

    private ModelBuilder modelBuilder;

    protected AbstractModelWrapper() {
        this.modelBuilder = new ModelBuilder();
    }


    @Override
    public Set<Namespace> getNamespaces() {
        return this.getModel().getNamespaces();
    }

    public ModelBuilder getBuilder() {
        return modelBuilder;
    }

    public Model getModel() {
        return modelBuilder.build();
    }

    protected void reset() {
        this.modelBuilder = new ModelBuilder();
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
