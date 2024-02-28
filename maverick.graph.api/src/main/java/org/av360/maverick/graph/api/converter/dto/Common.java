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

package org.av360.maverick.graph.api.converter.dto;

import org.av360.maverick.graph.model.rdf.Triples;
import org.av360.maverick.graph.model.vocabulary.meg.Local;
import org.av360.maverick.graph.model.vocabulary.meg.Metadata;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.io.Serializable;
import java.util.Map;
import java.util.stream.Collectors;

class Common {

    static Map<String, Serializable> getMetadata(Statement statement, Triples triples) {
        return triples.getModel()
                .stream()
                .filter(st -> st.getSubject().isTriple())
                .filter(st -> st.getSubject().equals(Values.triple(statement)))
                .filter(st -> st.getPredicate().getNamespace().equals(Metadata.NAMESPACE))
                .collect(Collectors.toMap(
                        st -> st.getPredicate().getLocalName(),
                        st -> st.getObject().stringValue(),
                        (left, right) -> {
                            return right;  // merge function, when duplicate, always take the right
                        }

                ));
    }

    static Map<String, Serializable> getDetails(Statement statement, Triples triples) {
        return triples.getModel()
                .stream()
                .filter(st -> st.getSubject().isTriple())
                .filter(st -> st.getSubject().equals(Values.triple(statement)))
                .filter(st -> ! st.getPredicate().getNamespace().equals(Metadata.NAMESPACE))
                .collect(Collectors.toMap(
                        st -> st.getPredicate().getLocalName(),
                        st -> st.getObject().stringValue(),
                        (left, right) -> {
                            return right;  // merge function, when duplicate, always take the right
                        }
                ));
    }

    public static boolean isRelationStatement(Statement statement, Triples triples) {
        return statement.getObject().isIRI()
                && ((IRI) statement.getObject()).getNamespace().startsWith(Local.URN_PREFIX)
                && ! triples.hasStatement((IRI) statement.getObject(), RDF.TYPE, Local.Entities.TYPE_EMBEDDED);

    }
}
