package com.bechtle.cougar.graph.repository;

import org.eclipse.rdf4j.model.Namespace;

import java.util.Optional;

public interface SchemaStore {
    Optional<Namespace> getNamespaceFor(String prefix);
}
