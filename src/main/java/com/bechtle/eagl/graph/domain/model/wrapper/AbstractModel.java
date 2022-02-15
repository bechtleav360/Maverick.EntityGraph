package com.bechtle.eagl.graph.domain.model.wrapper;

import com.bechtle.eagl.graph.domain.model.extensions.NamespaceAwareStatement;
import com.bechtle.eagl.graph.domain.model.extensions.NamespacedModelBuilder;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class AbstractModel implements NamespaceAware, Serializable {

    private NamespacedModelBuilder modelBuilder;

    protected AbstractModel(Model model) {
        this.modelBuilder = new NamespacedModelBuilder(model, Set.of());
    }

    protected AbstractModel() {
        this.modelBuilder = new NamespacedModelBuilder();
    }


    @Override
    public Set<Namespace> getNamespaces() {
        return this.getModel().getNamespaces();
    }

    public NamespacedModelBuilder getBuilder() {
        return modelBuilder;
    }

    public Model getModel() {
        return getBuilder().build();
    }

    public void reset() {
        this.modelBuilder = new NamespacedModelBuilder();
    }



    public Iterable<? extends NamespaceAwareStatement> asStatements() {
        return this.streamNamespaceAwareStatements().toList();
    }



    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        this.getModel().forEach(statement -> sb.append(statement).append('\n'));
        return sb.toString();
    }


    /**
     * Returns a list of object identifiers, which are embedded (means there is another object (entity)
     * within the model pointing to it
     */
    public Set<Resource> embeddedObjects() {
        Set<Resource> result = new HashSet<>();
        this.getModel().unmodifiable().objects().forEach(object -> {
                    this.getModel().stream()
                            .filter(statement -> statement.getObject().equals(object))
                            .filter(statement -> ! statement.getPredicate().equals(RDF.TYPE))
                            .findFirst()
                            .ifPresent(statement -> result.add(statement.getSubject()));
                }
        );
        return result;
    }

    public Stream<NamespaceAwareStatement> streamNamespaceAwareStatements() {
        return this.getModel().stream().map(sts -> NamespaceAwareStatement.wrap(sts, getNamespaces()));
    }

    public Stream<Statement> streamStatements(Resource subject, IRI predicate, Value object) {
        Iterable<Statement> statements = this.getModel().getStatements(subject, predicate, object);
        return StreamSupport.stream(statements.spliterator(), true);
    }


    public Stream<Value> streamValues(Resource subject, IRI predicate) {
        return this.streamStatements(subject, predicate, null).map(Statement::getObject);
    }

}
