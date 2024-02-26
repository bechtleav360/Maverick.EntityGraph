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

import org.av360.maverick.graph.api.controller.dto.Responses;
import org.av360.maverick.graph.model.rdf.Triples;
import org.av360.maverick.graph.model.vocabulary.meg.Local;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.util.Set;
import java.util.stream.Collectors;

public class RelationObjectConverter {

    public static Set<Responses.RelationObject> fromTriples(String entityKey, Triples triples) {
        return triples.streamStatements()
                .filter(statement -> statement.getSubject().isIRI() && statement.getObject().isIRI())
                .filter(statement -> ! statement.getPredicate().getNamespace().equalsIgnoreCase(RDF.NAMESPACE))
                .filter(statement -> ((IRI) statement.getObject()).getNamespace().startsWith(Local.URN_PREFIX))
                .filter(statement -> statement.getSubject().stringValue().endsWith(entityKey))
                .map(statement -> new Responses.RelationObject(
                        statement.getPredicate().stringValue(),
                        statement.getObject().stringValue(),
                        Common.getMetadata(statement, triples),
                        Common.getDetails(statement, triples)
                ))
                .collect(Collectors.toSet());
    }
}
