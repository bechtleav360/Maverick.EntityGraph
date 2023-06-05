package org.av360.maverick.graph.model.rdf;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.NamespaceAware;
import org.eclipse.rdf4j.model.Statement;

import java.io.Serializable;
import java.util.stream.Stream;

public interface Triples extends NamespaceAware, Serializable  {
    Stream<Statement> streamStatements();

    Model getModel();


}
