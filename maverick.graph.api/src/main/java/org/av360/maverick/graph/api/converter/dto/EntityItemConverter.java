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
import org.av360.maverick.graph.store.rdf.fragments.RdfFragment;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class EntityItemConverter {

    public static Responses.EntityResponse convert(RdfFragment fragment) {
        return new Responses.EntityResponse(
                getIdentifier(fragment),
                getScope(fragment),
                getKey(fragment), 
                getTypes(fragment),
                getMetadata(fragment),
                getValueObjects(fragment),
                getRelationObjects(fragment)
        ); 

    }

    private static String getKey(RdfFragment fragment) {
        String[] split = getLocalName(fragment.getIdentifier()).split("\\.");
        if(split.length == 2) {
            return split[1];
        } else return split[0];
    }

    private static String getScope(RdfFragment fragment) {
        String[] split = getLocalName(fragment.getIdentifier()).split("\\.");
        if(split.length == 2) {
            return split[0];
        } else return "default";
    }

    private static String getLocalName(Resource resource) {
        if(resource instanceof IRI iri) {
            return iri.getLocalName();
        } else return resource.stringValue();
    }

    private static Set<Responses.RelationObject> getRelationObjects(RdfFragment fragment) {
        return RelationObjectConverter.fromTriples(getKey(fragment), fragment);
    }

    private static Set<Responses.ValueObject> getValueObjects(RdfFragment fragment) {
        return ValueObjectConverter.fromTriples(getKey(fragment), fragment);
    }

    private static Map<String, String> getMetadata(RdfFragment fragment) {
        return Map.of();
    }

    private static String getIdentifier(RdfFragment fragment) {
        return fragment.getIdentifier().stringValue();
    }

    private static Set<String> getTypes(RdfFragment fragment) {
        return fragment.listStatements(fragment.getIdentifier(), RDF.TYPE, null)
                .stream()
                .map(Statement::getObject)
                .map(Value::stringValue)
                .collect(Collectors.toSet());
    }

}
