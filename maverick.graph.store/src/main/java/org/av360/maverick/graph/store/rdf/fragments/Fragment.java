package org.av360.maverick.graph.store.rdf.fragments;

import org.apache.commons.lang3.NotImplementedException;
import org.av360.maverick.graph.model.entities.Entity;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryResult;

/**
 * Represents a named graph of one particular entity.
 * <p>
 * Stores all items for one particular entity.
 */
public class Fragment extends TripleModel implements Entity {


    private final Resource identifier;

    public Fragment(Resource id) {
        super();
        identifier = id;
    }


    public Fragment(Resource resource, Model model) {
        super(model);
        this.identifier = resource;
    }


    public Fragment withResult(RepositoryResult<Statement> result) {
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



}
