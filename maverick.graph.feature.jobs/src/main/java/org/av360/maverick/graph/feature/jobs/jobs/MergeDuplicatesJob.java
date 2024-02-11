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

package org.av360.maverick.graph.feature.jobs.jobs;


import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.annotations.Job;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.entities.ScheduledJob;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.services.EntityServices;
import org.av360.maverick.graph.services.QueryServices;
import org.av360.maverick.graph.services.ValueServices;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Regular check for duplicates in the entity stores.
 * <p>
 * A typical example for a duplicate are the following two entities uploaded on different times
 * <p>
 * [] a ns1:VideoObject ;
 * ns1:hasDefinedTerm [
 * a ns1:DefinedTerm ;
 * rdfs:label "Term 1"
 * ] .
 * <p>
 * <p>
 * [] a ns1:VideoObject ;
 * ns1:hasDefinedTerm [
 * a ns1:DefinedTerm ;
 * rdfs:label "Term 1"
 * ] .
 * <p>
 * They both share the defined term "Term 1". Since they are uploaded in different requests, we don't check for duplicates. The (embedded) entity
 * <p>
 * x a DefinedTerm
 * label "Term 1"
 * <p>
 * is therefore a duplicate in the repository after the second upload. This scheduler will check for these duplicates by looking at objects which
 * - share the same label
 * - share the same original_identifier
 * <p>
 * <p>
 *  TODO:
 *      For now we keep the duplicate but reroute all links to the original.
 */
@Job
@Slf4j(topic = "graph.jobs.duplicates")
public class MergeDuplicatesJob implements ScheduledJob {

    public static String NAME = "detectDuplicates";
    private final EntityServices entityServices;
    private final QueryServices queryServices;
    private final ValueServices valueServices;
    private final SimpleValueFactory valueFactory;

    private final int limit = 100;

    private record Duplicates(Set<IRI> entities, IRI type, String sharedValue) {
        public TreeSet<IRI> sortedEntities() {
            return new TreeSet<>(entities);
        }
    }

    private record MislinkedStatement(IRI subject, IRI predicate, IRI object) {  }

    public MergeDuplicatesJob(EntityServices service, QueryServices queryServices, ValueServices valueServices) {
        this.entityServices = service;
        this.queryServices = queryServices;
        this.valueServices = valueServices;
        this.valueFactory = SimpleValueFactory.getInstance();
    }

    @Override
    public String getName() {
        return NAME;
    }

    public Mono<Void> run(SessionContext ctx) {

        return this.checkForDuplicates(ctx)
                .doOnError(throwable -> log.error("Exception while checking for duplicates: {}", throwable.getMessage()))
                .doOnSubscribe(sub -> {
                    ctx.updateEnvironment(env -> env.setRepositoryType(RepositoryType.ENTITIES));

                    log.trace("Checking for duplicates in environment {}.", ctx.getEnvironment());
                })
                .doOnSuccess(success -> log.debug("Completed checking for duplicates in environment {}.", ctx.getEnvironment()))
                .then();
    }

    private Mono<Void> checkForDuplicates(SessionContext ctx) {
        Scheduler scheduler = Schedulers.newSingle("duplicates");

        return this.findCandidates(ctx)
                .subscribeOn(scheduler)
                .map(candidate -> {
                    log.trace("There are multiple entities with shared type '{}' and label '{}'", candidate.type(), candidate.sharedValue());
                    return candidate;
                })
                .flatMap(duplicate -> this.mergeDuplicate(duplicate, ctx))
                .doOnSubscribe(sub -> log.debug("Checking duplicates sharing the same characteristic property in environment {}", ctx.getEnvironment()))
                .thenEmpty(Mono.empty());

    }


    /**
     * Method to merge the duplicates. We do the following steps
     * <p>
     * Take the head in list as original, the tail are the duplicates which are removed.
     * For each duplicate in the tail:
     * - find all statements pointing to the duplicate and reroute it to the original
     * - remove the duplicate
     * <p>
     * TODO: We remove all statements, no attempts are made to preserve additional statements in the duplicate. We should probably
     * a) keep the duplicate and mark it as deleted or
     * b) copy additional statements to the original or
     * c) keep the duplicate with most details as original
     *
     * @param duplicates
     * @param ctx
     * @return
     */
    private Mono<Void> mergeDuplicate(Duplicates duplicate, SessionContext ctx) {
        Optional<IRI> original = duplicate.entities().stream().findFirst();
        if (original.isEmpty()) return Mono.empty();
        SortedSet<IRI> deletionCandidates = duplicate.sortedEntities().tailSet(original.get(), false);

        /* relink */
        return Flux.fromIterable(deletionCandidates)
                .doOnSubscribe(subscription -> log.trace("Trying to merge all duplicates, keeping entity '{}' as original", original))
                .flatMap(duplicateIRI ->
                        this.findStatementsPointingToDuplicate(duplicateIRI, ctx)
                                .flatMap(mislinkedStatement -> this.relinkEntity(mislinkedStatement.subject(), mislinkedStatement.predicate(), mislinkedStatement.object(), original.get(), ctx))
                                .doOnNext(trx -> log.info("Relinking completed in Transaction {}", trx.getIdentifier()))
                                .map(transaction -> duplicateIRI)
                                .switchIfEmpty(Mono.just(duplicateIRI))
                )
                .flatMap(duplicateIRI ->
                        this.removeDuplicate(duplicateIRI, ctx)
                                .doOnNext(trx -> log.info("Removed duplicate entity with identifier '{}' in transaction '{}'", duplicateIRI, trx.getIdentifier()))
                )
                .then();

    }


    private Mono<Transaction> removeDuplicate(IRI object, SessionContext ctx) {
        // not sure if we should use the entity services (which handle authentication), or let the jobs always access only the store layer
        return this.entityServices.remove(object, ctx);
    }

    private Mono<Transaction> relinkEntity(IRI subject, IRI predicate, Value object, Value id, SessionContext ctx) {
        return this.valueServices.replace(subject, predicate, object, id, ctx);
    }

    /**
     * Runs the query:   SELECT * WHERE { ?sub ?pre <duplicate id> }. to find all entities pointing to the identified duplicate entity (with the goal to reroute those statements to the original)
     *
     * @param duplicate, the identified duplicate
     * @param ctx,       auth info
     * @return the statements pointing to the duplicate (as Flux)
     */
    private Flux<MislinkedStatement> findStatementsPointingToDuplicate(IRI duplicateIRI, SessionContext ctx) {
        Variable subject = SparqlBuilder.var("s");
        Variable predicate = SparqlBuilder.var("p");

        SelectQuery query = Queries.SELECT(subject, predicate).where(subject.has(predicate, duplicateIRI));
        return queryServices.queryValues(query, RepositoryType.ENTITIES, ctx)
                .doOnSubscribe(subscription -> log.trace("Retrieving all statements pointing to duplicate with id '{}'", duplicateIRI))
                .flatMap(binding -> {
                    Value pVal = binding.getValue(predicate.getVarName());
                    Value sVal = binding.getValue(subject.getVarName());

                    if (pVal.isIRI() && sVal.isIRI()) {
                        log.trace("Statement with subject identifier {} pointing with  predicate {} to the duplicate", sVal.stringValue(), pVal.stringValue());
                        return Mono.just(new MislinkedStatement((IRI) sVal, (IRI) pVal, duplicateIRI));
                    } else return Mono.empty();
                });

    }

    private Flux<Duplicates> findCandidates(SessionContext ctx) {

        String query = """
                PREFIX schema: <http://schema.org/>
                PREFIX sdo: <https://schema.org/>
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

                SELECT ?propertyValue ?type (GROUP_CONCAT(DISTINCT ?entity; separator=", ") AS ?duplicates)
                WHERE {
                  ?entity rdf:type ?type .
                  FILTER (!STRSTARTS(STR(?type), "urn:pwid:meg"))
                  OPTIONAL { ?entity <http://schema.org/name> ?schema_name . }
                  OPTIONAL { ?entity <https://schema.org/name> ?sdo_name . }
                  OPTIONAL { ?entity <http://schema.org/termCode> ?schema_termCode . }
                  OPTIONAL { ?entity <https://schema.org/termCode> ?sdo_termCode . }
                  OPTIONAL { ?entity <http://schema.org/identifier> ?schema_identifier . }
                  OPTIONAL { ?entity <https://schema.org/identifier> ?sdo_identifier . }
                  OPTIONAL { ?entity <http://purl.org/dc/elements/1.1/identifier> ?dc_identifier . }
                  OPTIONAL { ?entity <http://purl.org/dc/terms/identifier> ?dcterms_identifier . }
                  OPTIONAL { ?entity <http://www.w3.org/2004/02/skos/core#prefLabel> ?skos_prefLabel . }
                  OPTIONAL { ?entity <http://www.w3.org/2000/01/rdf-schema#label> ?rdfs_label . }
                  OPTIONAL { ?entity rdfs:label ?rdfs_label . }
                  OPTIONAL { ?entity rdfs:label ?rdfs_label . }
                  FILTER(BOUND(?schema_name) || BOUND(?rdfs_label) || BOUND(?schema_termCode)
                    || BOUND(?sdo_termCode) || BOUND(?schema_identifier) || BOUND(?sdo_identifier)
                    || BOUND(?dc_identifier) || BOUND(?dcterms_identifier) || BOUND(?skos_prefLabel)
                    || BOUND(?rdfs_label))
                  BIND(COALESCE(?schema_name, ?rdfs_label, ?schema_termCode, ?sdo_termCode, ?schema_identifier, ?sdo_identifier, ?dc_identifier, ?dcterms_identifier, ?skos_prefLabel, ?rdfs_label) AS ?propertyValue)
                }
                GROUP BY ?propertyValue ?type
                HAVING (COUNT(?entity) > 1)
                LIMIT %s
                                
                """.formatted(limit);

        Variable entitiesVariable = SparqlBuilder.var("duplicates");
        Variable propertyValueVariable = SparqlBuilder.var("propertyValue");
        Variable typeVariable = SparqlBuilder.var("type");

        return queryServices.queryValues(query, RepositoryType.ENTITIES, ctx)
                .map(binding -> {
                    String sharedValueVal = binding.getValue(propertyValueVariable.getVarName()).stringValue();
                    IRI typeVal = (IRI) binding.getValue(typeVariable.getVarName());
                    Set<IRI> entities = Arrays.stream(binding.getValue(entitiesVariable.getVarName()).stringValue().split(",")).map(Values::iri).collect(Collectors.toSet());

                    return new Duplicates(entities, typeVal, sharedValueVal);
                }).timeout(Duration.of(60, ChronoUnit.SECONDS));


    }





}

