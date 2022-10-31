package io.av360.maverick.graph.store.rdf.models;

import com.google.common.collect.Iterables;
import io.av360.maverick.graph.model.rdf.NamespaceAwareStatement;
import io.av360.maverick.graph.model.rdf.NamespacedModelBuilder;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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






    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        this.getModel().forEach(statement -> {
            sb.append(statement.getSubject()).append("  -  ");
            sb.append(statement.getPredicate()).append("  -  ");
            sb.append(statement.getObject()).append("  -  ");
            sb.append('\n');
        });
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

    public Stream<Statement> streamStatements(Resource subject, IRI predicate, Value object, Resource ... contexts) {
        Iterable<Statement> statements = this.getModel().getStatements(subject, predicate, object, contexts);
        return StreamSupport.stream(statements.spliterator(), true);
    }

    public Stream<Statement> streamStatements() {
        return this.streamStatements(null, null, null);
    }

    public List<Statement> listStatements(Resource subject, IRI predicate, Value object) {
        return this.streamStatements(subject, predicate, object).collect(Collectors.toList());
    }

    public Stream<Statement> streamStatements(Resource ... contexts) {
        return this.streamStatements(null, null, null, contexts);
    }

    public Iterable<NamespaceAwareStatement> asStatements() {
        return this.streamStatements().map(statement ->  NamespaceAwareStatement.wrap(statement, this.getNamespaces())).toList();
    }

    public Iterable<NamespaceAwareStatement> asStatements(Resource ... context) {
        return this.streamStatements(context).map(statement ->  NamespaceAwareStatement.wrap(statement, this.getNamespaces())).toList();
    }



    public Stream<Value> streamValues(Resource subject, IRI predicate) {
        return this.streamStatements(subject, predicate, null).map(Statement::getObject);
    }

    public boolean hasStatement(Resource obj, IRI pred, Value val) {
        return this.getModel().getStatements(obj, pred, val).iterator().hasNext();
    }


}
