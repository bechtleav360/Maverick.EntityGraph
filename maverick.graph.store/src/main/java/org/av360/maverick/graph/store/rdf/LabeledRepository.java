package org.av360.maverick.graph.store.rdf;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.base.RepositoryConnectionWrapper;
import org.eclipse.rdf4j.repository.base.RepositoryWrapper;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Required to override the toString() Method, to find out in the logs which repository has been used (to differentiate between the different applications)
 */
@Slf4j
public class LabeledRepository extends RepositoryWrapper {

    private final String label;

    private final Set<RepositoryConnection> connections;


    public LabeledRepository(String label, Repository repository) {
        super(repository);
        this.label = label;
        connections = Collections.newSetFromMap(new WeakHashMap<>());
    }

    @Override
    public String toString() {
        return this.label;
    }

    @Override
    public RepositoryConnection getConnection() throws RepositoryException {
        RepositoryConnectionWrapper connection = new RepositoryConnectionWrapper(this, super.getConnection());
        this.connections.add(connection);
        return connection;
    }

    public long getConnectionsCount() {
        long countAll = this.connections.stream().count();
        // long count = this.connections.stream().filter(RepositoryConnection::isOpen).count(); // is always zero
        return countAll;
    }
}
