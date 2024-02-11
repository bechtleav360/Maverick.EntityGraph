package org.av360.maverick.graph.store.rdf.fragments;

import org.av360.maverick.graph.model.rdf.Fragment;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryResult;

/**
 * Represents a named graph of one particular entity.
 * <p>
 * Stores all items for one particular entity.
 */
public class RdfFragment extends TripleModel implements Fragment {


    private final Resource identifier;

    public RdfFragment(Resource id) {
        super();
        identifier = id;
    }


    public RdfFragment(Resource resource, Model model) {
        super(model);
        this.identifier = resource;
    }


    public RdfFragment withResult(RepositoryResult<Statement> result) {
        try (result) {
            result.stream().forEach(statement -> this.getBuilder().add(statement.getSubject(), statement.getPredicate(), statement.getObject()));
        }
        return this;
    }

    public RdfFragment withModel(Model statements) {
        this.getBuilder().add(statements);
        return this;
    }






    public Resource getIdentifier() {
        return identifier;
    }

    @Override
    public boolean isIndividual() {
        return super.hasStatement(getIdentifier(), RDF.TYPE, Local.Entities.TYPE_INDIVIDUAL);
    }

    @Override
    public boolean isClassifier() {
        return super.hasStatement(getIdentifier(), RDF.TYPE, Local.Entities.TYPE_CLASSIFIER);
    }


}
