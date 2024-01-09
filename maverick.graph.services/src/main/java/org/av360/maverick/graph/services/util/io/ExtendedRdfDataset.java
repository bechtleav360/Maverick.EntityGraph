/*
 * Copyright (c) 2024.
 *
 *  Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 *  European Commission - subsequent versions of the EUPL (the "Licence");
 *
 *  You may not use this work except in compliance with the Licence.
 *  You may obtain a copy of the Licence at:
 *
 *  https://joinup.ec.europa.eu/software/page/eupl5
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the Licence for the specific language governing permissions and limitations under the Licence.
 *
 */

package org.av360.maverick.graph.services.util.io;

import com.apicatalog.rdf.*;

import java.util.*;
import java.util.stream.Collectors;

public class ExtendedRdfDataset implements RdfDataset {

    final RdfDataset delegate;
    final Set<Annotation> annotations;

    public Set<Annotation> getAnnotationsByIdentifierAndProperty(String identifier, String predicate) {
        return getAnnotations().stream()
                .filter(annotation -> annotation.subject().equalsIgnoreCase(identifier) && annotation.predicate().equalsIgnoreCase(predicate))
                .collect(Collectors.toSet());
    }

    public record Annotation(String subject, String predicate, String object, String annotationPredicate, String annotationValue) {}

    public void addAnnotation(String subject, String predicate, String object, String annotationPredicate, String annotationValue) {
        this.annotations.add(new Annotation(subject, predicate, object, annotationPredicate, annotationValue));
    }


    public Set<Annotation> getAnnotations() {
        return Collections.unmodifiableSet(annotations);
    }


    public Set<String> getNamespaces() {
        return namespaces;
    }

    final Set<String> namespaces;


    public ExtendedRdfDataset(RdfDataset delegate) {
        this.delegate = delegate;
        this.namespaces = new HashSet<>();
        this.annotations = new HashSet<>();
    }

    @Override
    public RdfGraph getDefaultGraph() {
        return delegate.getDefaultGraph();
    }

    public void registerNamespace(String namespace) {
        this.namespaces.add(namespace);
    }

    @Override
    public RdfDataset add(RdfNQuad nquad) {
        return delegate.add(nquad);
    }

    @Override
    public RdfDataset add(RdfTriple triple) {
        return delegate.add(triple);
    }

    @Override
    public List<RdfNQuad> toList() {
        return delegate.toList();
    }

    @Override
    public Set<RdfResource> getGraphNames() {
        return delegate.getGraphNames();
    }

    @Override
    public Optional<RdfGraph> getGraph(RdfResource graphName) {
        return delegate.getGraph(graphName);
    }

    @Override
    public int size() {
        return delegate.size();
    }
}
