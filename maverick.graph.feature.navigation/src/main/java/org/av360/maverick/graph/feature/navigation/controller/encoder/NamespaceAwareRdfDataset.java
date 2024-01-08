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

package org.av360.maverick.graph.feature.navigation.controller.encoder;

import com.apicatalog.rdf.*;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class NamespaceAwareRdfDataset implements RdfDataset {

    final RdfDataset delegate;

    public Set<String> getNamespaces() {
        return namespaces;
    }

    final Set<String> namespaces;


    public NamespaceAwareRdfDataset(RdfDataset delegate) {
        this.delegate = delegate;
        this.namespaces = new HashSet<>();
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
