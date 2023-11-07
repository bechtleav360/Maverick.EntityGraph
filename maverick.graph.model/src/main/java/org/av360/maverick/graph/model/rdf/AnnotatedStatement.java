package org.av360.maverick.graph.model.rdf;

import org.eclipse.rdf4j.model.*;

import java.util.Set;

/**
 * Simple Statement Wrapper which also keeps a pointer to the model (which gives access to the namespaces, these
 * are needed to write the correct headers in a response)
 */
public class AnnotatedStatement implements Statement, NamespaceAware {

    private final Statement statement;
    private final Set<Namespace> namespaces;

    private AnnotatedStatement(Statement statement, Set<Namespace> namespaces) {
        this.statement = statement;
        this.namespaces = namespaces;
    }

    public static AnnotatedStatement wrap(Statement statement, Set<Namespace> namespaces) {
        return new AnnotatedStatement(statement, namespaces);
    }

    @Override
    public Resource getSubject() {
        return this.statement.getSubject();
    }

    @Override
    public IRI getPredicate() {
        return this.statement.getPredicate();
    }

    @Override
    public Value getObject() {
        return this.statement.getObject();
    }

    @Override
    public Resource getContext() {
        return this.statement.getContext();
    }

    @Override
    public Set<Namespace> getNamespaces() {
        return this.namespaces;
    }
}
