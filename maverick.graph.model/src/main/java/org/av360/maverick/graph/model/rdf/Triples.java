package org.av360.maverick.graph.model.rdf;

import org.av360.maverick.graph.model.errors.InconsistentModelException;
import org.eclipse.rdf4j.model.*;

import java.io.Serializable;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Triples extends NamespaceAware, Serializable  {
    Collection<Fragment> listFragments();

    Stream<Statement> streamStatements(Resource... contexts);

    Optional<Statement> findStatement(Resource object, IRI predicate, Value value);
    boolean hasStatement(Resource object, IRI predicate, Value value);

    Model getModel();

    default Collection<AnnotatedStatement> asStatements() {
        return this.streamStatements().map(statement -> AnnotatedStatement.wrap(statement, this.getNamespaces())).collect(Collectors.toSet());
    }

    default Collection<AnnotatedStatement> asStatements(Resource... context) {
        return this.streamStatements(context).map(statement -> AnnotatedStatement.wrap(statement, this.getNamespaces())).collect(Collectors.toSet());
    }


    default Stream<Statement> streamStatements() {
        return this.streamStatements(null, null, null);
    }

    void reduce(Predicate<Statement> filterFunction);

    Triples filter(Predicate<Statement> filterFunction);

    Stream<Value> streamValues(Resource subject, IRI predicate);

    Value getDistinctValue(Resource subject, IRI predicate) throws NoSuchElementException, InconsistentModelException;

    Optional<Value> findDistinctValue(Resource subject, IRI predicate) throws  InconsistentModelException;



    boolean hasStatement(Triple triple);
}
