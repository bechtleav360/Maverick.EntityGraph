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

package org.av360.maverick.graph.services.api.entities.capabilities;

import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.errors.InconsistentModelException;
import org.av360.maverick.graph.model.vocabulary.SDO;
import org.av360.maverick.graph.services.api.Api;
import org.av360.maverick.graph.store.IndividualsStore;
import org.av360.maverick.graph.store.rdf.fragments.RdfFragment;
import org.av360.maverick.graph.store.rdf.helpers.BindingsAccessor;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class FindEntities {

    private final Api api;
    private final IndividualsStore individualsStore;

    public FindEntities(Api api, IndividualsStore individualsStore) {
        this.api = api;
        this.individualsStore = individualsStore;
    }

    public Flux<RdfFragment> list(int limit, int offset, SessionContext ctx, String query) {

        if(! StringUtils.hasLength(query)) {
            query = """
                        
                    PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                    PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
                    PREFIX sdo: <https://schema.org/>
                    PREFIX schema: <http://schema.org/>
                    PREFIX dcterms: <http://purl.org/dc/terms/>

                    SELECT ?id ?sct ?dct ?rdt ?skt (GROUP_CONCAT(DISTINCT ?type; SEPARATOR=",") AS ?types)
                    WHERE
                    {
                      {
                        SELECT ?id WHERE {
                          ?id a <urn:pwid:meg:e:Individual> .
                        }
                        LIMIT $limit
                        OFFSET $offset
                      }
                      OPTIONAL { ?id sdo:title ?sct }.
                      OPTIONAL { ?id sdo:name ?sct }.
                      OPTIONAL { ?id schema:title ?sct }.
                      OPTIONAL { ?id schema:name ?sct }.                      
                      OPTIONAL { ?id dcterms:title ?dct }.
                      OPTIONAL { ?id rdfs:label ?rdt }.
                      OPTIONAL { ?id skos:prefLabel ?skt }.
                      ?id a ?type .
                    }
                    GROUP BY ?id  ?sct ?dct ?rdt ?skt
                """;
        }
        query = query.replace("$limit", limit + "").replace("$offset", offset + "");

        return individualsStore.asSearchable().query(query, ctx.getEnvironment())
                .map(BindingsAccessor::new)
                .flatMap(bnd -> {
                    try {
                        Resource resource = bnd.asIRI("id");

                        ModelBuilder builder = new ModelBuilder();
                        builder.subject(resource);
                        bnd.asSet("types", ",").stream()
                                .map(typeString -> SimpleValueFactory.getInstance().createIRI(typeString))
                                .forEach(typeIRI -> builder.add(RDF.TYPE, typeIRI));
                        bnd.findValue("sct").ifPresent(val -> {
                            builder.add(SDO.TITLE, val);
                            builder.setNamespace(SDO.NS);
                        });
                        bnd.findValue("rdt").ifPresent(val -> {
                            builder.add(RDFS.LABEL, val);
                            builder.setNamespace(RDFS.NS);
                        });
                        bnd.findValue("dct").ifPresent(val -> {
                            builder.add(DCTERMS.TITLE, val);
                            builder.setNamespace(DCTERMS.NS);
                        });
                        bnd.findValue("skt").ifPresent(val -> {
                            builder.add(SKOS.PREF_LABEL, val);
                            builder.setNamespace(SKOS.NS);
                        });

                        return Mono.just(new RdfFragment(resource, builder.build()));
                    } catch (InconsistentModelException e) {
                        return Mono.error(e);
                    }
                });
    }


    public Mono<RdfFragment> findByProperty(String identifier, IRI predicate, boolean details, int depth, SessionContext ctx) {
        Literal identifierLit = Values.literal(identifier);

        Variable idVariable = SparqlBuilder.var("id");

        SelectQuery query = Queries.SELECT(idVariable).where(
                idVariable.has(predicate, identifierLit));

        return this.individualsStore.asSearchable().query(query, ctx.getEnvironment())
                .next()
                .map(bindings -> bindings.getValue(idVariable.getVarName()))
                .filter(Value::isResource)
                .flatMap(entityIdentifier -> api.entities().select().get((Resource) entityIdentifier, details, depth, ctx));
    }

    public Mono<Long> count(SessionContext ctx) {
        return this.individualsStore.asFragmentable().countFragments(ctx.getEnvironment());
    }
}
