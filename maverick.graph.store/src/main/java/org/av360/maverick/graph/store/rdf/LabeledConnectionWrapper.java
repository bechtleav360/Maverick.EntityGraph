package org.av360.maverick.graph.store.rdf;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.base.RepositoryConnectionWrapper;

public class LabeledConnectionWrapper extends RepositoryConnectionWrapper {


    public LabeledConnectionWrapper(Repository repository, RepositoryConnection delegate) {
        super(repository, delegate);
    }
}
