package org.av360.maverick.graph.store.rdf.fragments;

import org.av360.maverick.graph.model.errors.InconsistentModelException;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.model.rdf.Fragment;
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




    @Override
    public Collection<Fragment> listFragments() {
        Set<Fragment> result = new HashSet<>();
        this.getModel().subjects().forEach(subject ->  {
            Model filteredModel = this.getModel().filter(subject, null, null);
            RdfFragment rdfFragment = new RdfFragment(subject).withModel(filteredModel);

            result.add(rdfFragment);
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

    public List<Statement> listStatements() {
        return this.streamStatements(null, null, null).collect(Collectors.toList());
    }

    public Stream<Statement> streamStatements(Resource... contexts) {
        return this.streamStatements(null, null, null, contexts);
    }



    @Override
    public void reduce(Predicate<Statement> filterFunction) {
        Set<Statement> collect = this.streamStatements().filter(filterFunction).collect(Collectors.toSet());
        this.getModel().clear();;
        this.getModel().addAll(collect);
    }

    @Override
    public Triples filter(Predicate<Statement> filterFunction) {
        this.reduce(filterFunction);
        return (Triples) this;
    }







    @Override
    public Stream<Value> streamValues(Resource subject, IRI predicate) {
        return this.streamStatements(subject, predicate, null).map(Statement::getObject);
    }

    @Override
    public Value getDistinctValue(Resource subject, IRI predicate) throws NoSuchElementException, InconsistentModelException {
        return this.findDistinctValue(subject, predicate).orElseThrow();
    }

    @Override
    public Optional<Value> findDistinctValue(Resource subject, IRI predicate) throws  InconsistentModelException {
        Set<Value> collect = this.streamValues(subject, predicate).collect(Collectors.toUnmodifiableSet());
        if(collect.isEmpty()) return Optional.empty();
        else if(collect.size() == 1) return collect.stream().findFirst();
        else throw new InconsistentModelException("Multiple values found for predicte '%s'".formatted(predicate));
    }


    @Override
    public boolean hasStatement(Resource obj, IRI pred, Value val) {
        return this.getModel().getStatements(obj, pred, val).iterator().hasNext();
    }


    @Override
    public Optional<Statement> findStatement(Resource object, IRI predicate, Value value) {
        Iterator<Statement> itr = this.getModel().getStatements(object, predicate, value).iterator();

        if(itr.hasNext()) {
            return Optional.of(itr.next());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public boolean hasStatement(Triple triple) {
        return this.hasStatement(triple.getSubject(), triple.getPredicate(), triple.getObject());

    }
}
