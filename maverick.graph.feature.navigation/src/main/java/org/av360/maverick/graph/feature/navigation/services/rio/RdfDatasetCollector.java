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

package org.av360.maverick.graph.feature.navigation.services.rio;

import com.apicatalog.rdf.RdfNQuad;
import com.apicatalog.rdf.RdfResource;
import com.apicatalog.rdf.RdfTriple;
import com.apicatalog.rdf.RdfValue;
import com.apicatalog.rdf.impl.DefaultRdfProvider;
import com.apicatalog.rdf.spi.RdfProvider;
import org.av360.maverick.graph.feature.navigation.controller.encoder.NamespaceAwareRdfDataset;
import org.eclipse.rdf4j.model.*;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class RdfDatasetCollector implements Collector<Statement, NamespaceAwareRdfDataset, NamespaceAwareRdfDataset> {

    private final RdfProvider rdfProvider;

    public RdfDatasetCollector(RdfProvider rdfProvider) {
        this.rdfProvider = rdfProvider;
    }

    public static RdfDatasetCollector toDataset() {
        return new RdfDatasetCollector(new DefaultRdfProvider());
    }

    @Override
    public Supplier<NamespaceAwareRdfDataset> supplier() {
        return () -> new NamespaceAwareRdfDataset(rdfProvider.createDataset());
    }

    @Override
    public BiConsumer<NamespaceAwareRdfDataset, Statement> accumulator() {
        return (m, s) -> {
            synchronized (m) {
                this.extractNamespaces(s, m);
                RdfNQuad quad = this.convert(s);
                if(Objects.nonNull(quad)) {
                    m.add(quad);
                }

            }
        };
    }

    private void extractNamespaces(Statement s, NamespaceAwareRdfDataset m) {
        m.registerNamespace(s.getPredicate().getNamespace());
        if(s.getObject() instanceof IRI iri) m.registerNamespace(iri.getNamespace());
        if(s.getSubject() instanceof IRI iri) m.registerNamespace(iri.getNamespace());

    }

    @Override
    public BinaryOperator<NamespaceAwareRdfDataset> combiner() {
        return (m1, m2) -> {
            m2.toList().forEach(m1::add);
            return m1;
        };
    }

    @Override
    public Function<NamespaceAwareRdfDataset, NamespaceAwareRdfDataset> finisher() {
        return Function.identity();
    }

    @Override
    public Set<Characteristics> characteristics() {
        return EnumSet.of(Characteristics.CONCURRENT, Characteristics.IDENTITY_FINISH, Characteristics.UNORDERED);
    }

    private RdfTriple convert(Triple triple) {
        Resource tripleSubject = triple.getSubject();
        IRI triplePredicate = triple.getPredicate();
        Value tripleValue = triple.getObject();

        RdfResource convertedSubject = null;
        RdfResource convertedPredicate = null;
        RdfValue convertedObject = null;
        return null;
    }

    private RdfNQuad convert(Statement statement) {
        Resource subject = statement.getSubject();
        IRI predicate = statement.getPredicate();
        Value object = statement.getObject();
        Resource context = statement.getContext();

        RdfResource convertedSubject = null;
        RdfTriple convertedSubjectTriple = null;
        RdfResource convertedPredicate = null;
        RdfValue convertedObject = null;
        RdfResource convertedContext = null;

        if (subject.isBNode()) {
            convertedSubject = rdfProvider.createBlankNode(subject.stringValue());
        } else if (subject.isIRI()) {
            convertedSubject = rdfProvider.createIRI(subject.stringValue());
        } else if(subject instanceof Triple triple) {
            Resource tripleSubject = triple.getSubject();
            IRI triplePredicate = triple.getPredicate();
            Literal tripleValue = (Literal) triple.getObject();

            RdfResource convertedTripleSubject = rdfProvider.createIRI(tripleSubject.stringValue());
            RdfResource convertedTriplePredicate = rdfProvider.createIRI(triplePredicate.stringValue());
            RdfValue convertedTripleObject = rdfProvider.createTypedString(tripleValue.stringValue(), tripleValue.getDatatype().stringValue());

            convertedSubjectTriple = rdfProvider.createTriple(convertedTripleSubject, convertedTriplePredicate, convertedTripleObject);

        }

        if (predicate.isIRI()) {
            convertedPredicate = rdfProvider.createIRI(predicate.stringValue());
        }

        if (object instanceof IRI iri) {
            convertedObject = rdfProvider.createIRI(iri.stringValue());
        } else if (object instanceof Literal literal) {
            if (literal.getLanguage().isPresent()) {
                convertedObject = rdfProvider.createLangString(literal.getLabel(), literal.getLanguage().get());
            } else {
                convertedObject = rdfProvider.createTypedString(literal.getLabel(), literal.getDatatype().stringValue());
            }
        } else if (object instanceof BNode blank) {
            convertedObject = rdfProvider.createBlankNode(blank.stringValue());
        }

        if(context instanceof IRI iri) {
            convertedContext = rdfProvider.createIRI(iri.stringValue());
        }
        if(Objects.nonNull(convertedSubject)) {
            return rdfProvider.createNQuad(convertedSubject, convertedPredicate, convertedObject, convertedContext);
        } else {
            return null;
        }

    }
}
