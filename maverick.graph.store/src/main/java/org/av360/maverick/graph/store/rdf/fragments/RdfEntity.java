package org.av360.maverick.graph.store.rdf.fragments;

import org.apache.commons.lang3.NotImplementedException;
import org.av360.maverick.graph.model.entities.Entity;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryResult;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Represents a named graph of one particular entity.
 * <p>
 * Stores all items for one particular entity.
 */
public class RdfEntity extends TripleModel implements Entity {


    private final Resource identifier;

    public RdfEntity(Resource id) {
        super();
        identifier = id;
    }

    public RdfEntity withResult(RepositoryResult<Statement> result) {
        try (result) {
            result.stream().forEach(statement -> this.getBuilder().add(statement.getSubject(), statement.getPredicate(), statement.getObject()));
        }
        return this;
    }

    boolean isShared() {
        throw new NotImplementedException();
    }


    public Resource getIdentifier() {
        return identifier;
    }

    public void filter(Predicate<Statement> filterFunction) {
        Set<Statement> collect = this.streamStatements().filter(filterFunction).collect(Collectors.toSet());
        this.getModel().clear();;
        this.getModel().addAll(collect);
    }
}
