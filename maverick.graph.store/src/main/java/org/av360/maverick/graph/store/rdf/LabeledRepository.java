package org.av360.maverick.graph.store.rdf;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.base.RepositoryConnectionWrapper;
import org.eclipse.rdf4j.repository.base.RepositoryWrapper;

/**
 * Required to override the toString() Method, to find out in the logs which repository has been used (to differentiate between the different applications)
 */
public class LabeledRepository extends RepositoryWrapper {

    private final String label;

    public LabeledRepository(String label, Repository repository) {
        super(repository);
        this.label = label;
    }

    @Override
    public String toString() {
        return this.label;
    }

    @Override
    public RepositoryConnection getConnection() throws RepositoryException {
        return new RepositoryConnectionWrapper(this, super.getConnection());
    }
}
