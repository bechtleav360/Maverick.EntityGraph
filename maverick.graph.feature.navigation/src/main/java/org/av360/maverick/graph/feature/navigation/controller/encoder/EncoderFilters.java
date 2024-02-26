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

import org.av360.maverick.graph.model.vocabulary.meg.Local;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.ModelCollector;
import org.eclipse.rdf4j.model.util.Statements;
import org.eclipse.rdf4j.model.util.Values;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.util.function.Tuple2;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class EncoderFilters {


    static Model convertCompositesToBlankNodes(Model statements) {
        Set<Resource> subjects = statements.filter(null, null, Local.Entities.TYPE_EMBEDDED).subjects();
        // FIXME: check if multiple pointers to embedded (is inconsistent, but can happen)
        Map<Resource, BNode> mappings = subjects.stream().collect(Collectors.toMap(str -> str, str -> Values.bnode()));


        ModelBuilder modelBuilder = new ModelBuilder();
        statements.forEach(statement -> {
            if (subjects.contains(statement.getSubject())) {
                modelBuilder.add(mappings.get(statement.getSubject()), statement.getPredicate(), statement.getObject());
            } else if (statement.getObject().isResource() && subjects.contains((Resource) statement.getObject())) {
                modelBuilder.add(statement.getSubject(), statement.getPredicate(), mappings.get((Resource) statement.getObject()));
            } else {
                modelBuilder.add(statement.getSubject(), statement.getPredicate(), statement.getObject());
            }
        });
        return modelBuilder.build();
    }

    static boolean filterInternalTypeStatements(Statement statement) {
        return EncoderFilters.isLiteralWithCommonLanguageTag(statement)
                && !statement.getObject().equals(Local.Entities.TYPE_INDIVIDUAL)
                && !statement.getObject().equals(Local.Entities.TYPE_CLASSIFIER)
                && !statement.getObject().equals(Local.Entities.TYPE_EMBEDDED)
                && !statement.getPredicate().equals(Local.ORIGINAL_IDENTIFIER)
                ;
    }

    static Model filterInternalTypeStatements(Model model) {
        // we filter out any internal statements
        return model.stream()
                .filter(EncoderFilters::filterInternalTypeStatements)
                .collect(new ModelCollector());
    }

    private static boolean isLiteralWithCommonLanguageTag(Statement statement) {
        if (statement.getObject() instanceof Literal literal) {
            if (literal.getLanguage().isPresent()) {
                return literal.getLanguage().map(lang -> lang.startsWith("en") || lang.startsWith("de") || lang.startsWith("fr") || lang.startsWith("es")).orElse(Boolean.FALSE);
            } else return true;
        }
        return true;
    }

    public static Statement resolveURLs(Statement statement, URI requestURI) {
        statement = handleLocalIdentifiers(statement, requestURI);
        statement = handleLiterals(statement, requestURI);

        return statement;
    }

    private static Statement handleLocalIdentifiers(Statement statement, URI requestURI) {
        Resource subject = convertLocalIRI(statement.getSubject(), statement, requestURI);
        Value object = convertLocalIRI(statement.getObject(), statement, requestURI);

        return Statements.statement(subject, statement.getPredicate(), object, statement.getContext());
    }

    @SuppressWarnings("unchecked")
    private static <T extends Value> T convertLocalIRI(T value, Statement statement, URI requestURI) {
        if(value instanceof Triple triple) {
            return (T) Values.triple(
                    convertLocalIRI(triple.getSubject(), statement, requestURI),
                    triple.getPredicate(),
                    convertLocalIRI(triple.getObject(), statement, requestURI)
            );
        }


        if (value instanceof IRI iri) {
            if (iri.getNamespace().startsWith(Local.URN_PREFIX)) {

                /* ok, here the application module is leaking into the default implementation. If the IRI namespace includes a qualifier,
                   we inject it as scope. To make it reproducible, we assume: if namespace is not default namespace, we take the (URL decoded) string
                   attached and place it under scope ("s").

                   Examples:
                    urn:pwid:meg:e:,213 -> /api/entities/213
                    urn:pwid:meg:e:, f33.213 -> /api/entities/s/f33/213
                    urn:pwid:meg:, 123 ->  /api/
                 */
                String[] parts = iri.getLocalName().split("\\.");
                String ns = iri.getNamespace();
                String path = "";

                if (ns.startsWith(Local.Entities.NAME)) {
                    if (parts.length == 1) {
                        path += "/api/entities/" + parts[0];
                    } else {
                        path += "/api/s/" + parts[0] + "/entities/" + parts[1];
                    }

                } else if (iri.getNamespace().startsWith(Local.Transactions.NAME)) {
                    path = "/api/transactions/" + parts[0];
                } else if (iri.getNamespace().startsWith(Local.Applications.NAME)) {
                    path = "/api/applications/" + parts[0];
                } else {
                    path = parts[0];
                }

                String uri = UriComponentsBuilder.fromUri(requestURI).replacePath(path).replaceQuery("").build().toUriString();
                return (T) Values.iri(uri);
            }
        }
        return value;
    }

    private static Statement handleLiterals(Statement statement, URI requestURI) {
        if (statement.getObject() instanceof Literal literal) {
            if (literal.stringValue().startsWith("?/")) {
                String path = literal.stringValue();
                String uri = UriComponentsBuilder.fromUri(requestURI).replacePath(path.substring(1)).replaceQuery("").build().toUriString();
                return Statements.statement(statement.getSubject(), statement.getPredicate(), Values.literal(uri), statement.getContext());
            }

            if (literal.stringValue().startsWith("/content")) {
                String path = literal.stringValue();
                String uri = UriComponentsBuilder.fromUri(requestURI).replacePath(path.substring(1)).replaceQuery("").build().toUriString();
                return Statements.statement(statement.getSubject(), statement.getPredicate(), Values.literal(uri), statement.getContext());
            }
        }
        return statement;

    }

    public static Statement resolveURLs(Tuple2<? extends Statement, URI> tuple) {
        return resolveURLs(tuple.getT1(), tuple.getT2());
    }
}
