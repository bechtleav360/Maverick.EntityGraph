package org.av360.maverick.graph.store.rdf4j.extensions;

import org.av360.maverick.graph.store.repository.GraphStore;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.base.RepositoryConnectionWrapper;
import org.eclipse.rdf4j.repository.base.RepositoryWrapper;

import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Required to override the toString() Method, to find out in the logs which repository has been used (to differentiate between the different applications)
 */
public class LabeledRepository extends RepositoryWrapper implements GraphStore {

    private final String label;

    private final WeakHashMap<String, RepositoryConnection> connections;

    private final AtomicInteger counter;

    public LabeledRepository(String label, Repository repository) {
        super(repository);
        this.label = label;
        connections = new WeakHashMap<>();
        this.counter = new AtomicInteger();
    }

    @Override
    public String toString() {
        return this.label;
    }

    @Override
    public RepositoryConnection getConnection() throws RepositoryException {
        RepositoryConnectionWrapper connection = new RepositoryConnectionWrapper(this, super.getConnection());
        this.connections.put(this.label+counter.incrementAndGet(), connection);
        return connection;
    }

    public int getConnectionsCount() {
        return this.connections.size();
    }
}
