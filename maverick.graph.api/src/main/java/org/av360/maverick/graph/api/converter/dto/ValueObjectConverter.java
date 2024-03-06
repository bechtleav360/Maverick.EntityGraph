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
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.io.Serializable;
import java.util.Set;
import java.util.stream.Collectors;

public class ValueObjectConverter {


    public static Responses.ValueObject getFromTriples(String entityKey, Triples triples) {
        return triples.streamStatements()
                .filter(statement -> statement.getSubject().isIRI())
                .filter(statement -> statement.getSubject().stringValue().endsWith(entityKey))
                .filter(statement -> !statement.getPredicate().getNamespace().equalsIgnoreCase(RDF.NAMESPACE))
                .filter(statement -> !Common.isRelationStatement(statement, triples))
                .map(statement -> new Responses.ValueObject(
                        statement.getPredicate().stringValue(),
                        getValue(statement.getObject(), triples),
                        getLanguage(statement.getObject()),
                        Common.getMetadata(statement, triples),
                        Common.getDetails(statement, triples)
                ))
                .findFirst().orElseThrow();
    }

    public static Set<Responses.ValueObject> listFromTriples(String entityKey, Triples triples) {
        return triples.streamStatements()
                .filter(statement -> statement.getSubject().isIRI())
                .filter(statement -> statement.getSubject().stringValue().endsWith(entityKey))
                .filter(statement -> !statement.getPredicate().getNamespace().equalsIgnoreCase(RDF.NAMESPACE))
                .filter(statement -> !Common.isRelationStatement(statement, triples))
                .map(statement -> new Responses.ValueObject(
                        statement.getPredicate().stringValue(),
                        getValue(statement.getObject(), triples),
                        getLanguage(statement.getObject()),
                        Common.getMetadata(statement, triples),
                        Common.getDetails(statement, triples)
                ))

                .collect(Collectors.toSet());
    }

    private static String getLanguage(Value object) {
        if (object.isLiteral()) {
            return ((Literal) object).getLanguage().orElse(null);
        } else return null;
    }

    private static Serializable getValue(Value object, Triples triples) {
        if (object.isLiteral()) {
            return getLiteral((Literal) object);
        } else if (object.isIRI()) {
            return getEmbedded((IRI) object, triples);
        }

        return object.stringValue();
    }

    private static Serializable getEmbedded(IRI object, Triples triples) {
        return object.stringValue();
    }

    private static Serializable getLiteral(Literal literal) {
        if (literal.getCoreDatatype().equals(CoreDatatype.XSD.BOOLEAN)) {
            return literal.booleanValue();
        } else if (literal.getCoreDatatype().equals(CoreDatatype.XSD.DECIMAL)) {
            return literal.decimalValue();
        } else if (literal.getCoreDatatype().equals(CoreDatatype.XSD.DOUBLE)) {
            return literal.doubleValue();
        } else if (literal.getCoreDatatype().equals(CoreDatatype.XSD.INTEGER)) {
            return literal.integerValue();
        } else {
            return literal.stringValue();
        }
    }


}
