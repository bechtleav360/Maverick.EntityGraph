package org.av360.maverick.graph.store.rdf.fragments;

import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.model.rdf.Triples;
import org.av360.maverick.graph.store.rdf.helpers.NamespacedModelBuilder;
import org.eclipse.rdf4j.model.*;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class TripleModel implements Triples {

    private NamespacedModelBuilder modelBuilder;

    public TripleModel(Model model) {
        this.modelBuilder = new NamespacedModelBuilder(model, Set.of());
    }

    protected TripleModel() {
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




    public Map<Resource, Model> listFragments() {
        Map<Resource, Model> result = new HashMap<>();
        this.getModel().subjects().forEach(subject ->  {
            result.put(subject, this.getModel().filter(subject, null, null));
        });
        return result;
    }

    public Stream<AnnotatedStatement> streamNamespaceAwareStatements() {
        return this.getModel().stream().map(sts -> AnnotatedStatement.wrap(sts, getNamespaces()));
    }

    public Stream<Statement> streamStatements(Resource subject, IRI predicate, Value object) {
        Iterable<Statement> statements = this.getModel().getStatements(subject, predicate, object);
        return StreamSupport.stream(statements.spliterator(), true);
    }

    public Stream<Statement> streamStatements(Resource subject, IRI predicate, Value object, Resource... contexts) {
        Iterable<Statement> statements = this.getModel().getStatements(subject, predicate, object, contexts);
        return StreamSupport.stream(statements.spliterator(), true);
    }


    public List<Statement> listStatements(Resource subject, IRI predicate, Value object) {
        return this.streamStatements(subject, predicate, object).collect(Collectors.toList());
    }

    public Stream<Statement> streamStatements(Resource... contexts) {
        return this.streamStatements(null, null, null, contexts);
    }

    public void reduce(Predicate<Statement> filterFunction) {
        Set<Statement> collect = this.streamStatements().filter(filterFunction).collect(Collectors.toSet());
        this.getModel().clear();;
        this.getModel().addAll(collect);
    }







    public Stream<Value> streamValues(Resource subject, IRI predicate) {
        return this.streamStatements(subject, predicate, null).map(Statement::getObject);
    }

    public Value getDistinctValue(Resource subject, IRI predicate) throws NoSuchElementException {
        return this.findDistinctValue(subject, predicate).orElseThrow();
    }

    public Optional<Value> findDistinctValue(Resource subject, IRI predicate) {
        return this.streamValues(subject, predicate).findFirst();
    }


    public boolean hasStatement(Resource obj, IRI pred, Value val) {
        return this.getModel().getStatements(obj, pred, val).iterator().hasNext();
    }


    public boolean hasStatement(Triple triple) {
        return this.hasStatement(triple.getSubject(), triple.getPredicate(), triple.getObject());

    }
}
