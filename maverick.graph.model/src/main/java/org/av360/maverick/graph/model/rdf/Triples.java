package org.av360.maverick.graph.model.rdf;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.NamespaceAware;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;

import java.io.Serializable;
import java.util.stream.Stream;

public interface Triples extends NamespaceAware, Serializable  {
    Stream<Statement> streamStatements(Resource... contexts);

    Model getModel();


    default Iterable<AnnotatedStatement> asStatements() {
        return this.streamStatements().map(statement -> AnnotatedStatement.wrap(statement, this.getNamespaces())).toList();
    }

    default Iterable<AnnotatedStatement> asStatements(Resource... context) {
        return this.streamStatements(context).map(statement -> AnnotatedStatement.wrap(statement, this.getNamespaces())).toList();
    }


    default Stream<Statement> streamStatements() {
        return this.streamStatements(null, null, null);
    }
}
