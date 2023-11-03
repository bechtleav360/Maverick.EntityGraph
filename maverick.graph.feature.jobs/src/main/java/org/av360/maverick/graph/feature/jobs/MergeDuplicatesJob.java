package org.av360.maverick.graph.feature.jobs;


import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.aspects.Job;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.entities.ScheduledJob;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.model.vocabulary.SCHEMA;
import org.av360.maverick.graph.model.vocabulary.SDO;
import org.av360.maverick.graph.services.EntityServices;
import org.av360.maverick.graph.services.QueryServices;
import org.av360.maverick.graph.services.ValueServices;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

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

    private final int limit = 10;

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



        return this.checkForDuplicates(RDFS.LABEL, ctx)
                .then(this.checkForDuplicates(OWL.SAMEAS, ctx))
                .then(this.checkForDuplicates(SDO.IDENTIFIER, ctx))
                .then(this.checkForDuplicates(SDO.TERM_CODE, ctx))
                .then(this.checkForDuplicates(SDO.NAME, ctx))
                .then(this.checkForDuplicates(SDO.URL, ctx))
                .then(this.checkForDuplicates(SCHEMA.IDENTIFIER, ctx))
                .then(this.checkForDuplicates(SCHEMA.TERM_CODE, ctx))
                .then(this.checkForDuplicates(SCHEMA.NAME, ctx))
                .then(this.checkForDuplicates(SCHEMA.URL, ctx))
                .then(this.checkForDuplicates(SKOS.PREF_LABEL, ctx))
                .then(this.checkForDuplicates(DCTERMS.IDENTIFIER, ctx))
                .then(this.checkForDuplicates(DC.IDENTIFIER, ctx))
                .doOnError(throwable -> log.error("Exception while checking for duplicates: {}", throwable.getMessage()))
                .doOnSubscribe(sub -> {
                    ctx.updateEnvironment(env -> env.setRepositoryType(RepositoryType.ENTITIES));

                    log.trace("Checking for duplicates in environment {}.", ctx.getEnvironment());
                })
                .doOnSuccess(success -> log.debug("Completed checking for duplicates in environment {}.", ctx.getEnvironment()))
                .then();


    }

    private Mono<Void> checkForDuplicates(IRI characteristicProperty, SessionContext ctx) {
        Scheduler scheduler = Schedulers.newSingle("duplicates_" + characteristicProperty.getLocalName());

        return this.findCandidates(characteristicProperty, ctx)
                .subscribeOn(scheduler)
                .map(candidate -> {
                    log.trace("There are multiple entities with shared type '{}' and label '{}'", candidate.type(), candidate.sharedValue());
                    return candidate;
                })
                .flatMap(candidate ->
                        this.findDuplicates(candidate, ctx)
                                .doOnNext(duplicate -> log.trace("Entity '{}' identified as duplicate. Another item exists with property {} and value {} .", duplicate.id(), candidate.sharedProperty(), candidate.sharedValue()))
                                .collectList()
                                .flatMap(duplicates -> this.mergeDuplicates(duplicates, ctx))
                                .then(Mono.just(candidate))
                )
                .collectList()
                .doOnNext(list -> {
                    if(list.size() > 0) {
                        log.debug("Found {} candidates for duplicates for property {}", list.size(), characteristicProperty);
                    }
                })
                .doOnNext(list -> {
                    if(list.size() == this.limit) {
                        this.checkForDuplicates(characteristicProperty, ctx).subscribeOn(scheduler).subscribe();
                    }
                })
                .doOnSubscribe(sub ->
                        log.debug("Checking duplicates sharing the same characteristic property <{}> in environment {}", characteristicProperty, ctx.getEnvironment())
                )

                .thenEmpty(Mono.empty());

    }

    private Flux<Duplicate> findDuplicates(DuplicateCandidate duplicate, SessionContext ctx) {

        /*
        select ?thing where {
                ?thing 	a <http://schema.org/video>.
  ?thing 	<http://schema.org/identifier> "_a1238" .
    ?thing 	<http://schema.org/dateCreated> ?date .
}
         */
        /*

        SELECT ?id WHERE {
   ?id a <http://schema.org/Organization> .
   ?id <http://schema.org/name> ?val .
   FILTER regex(?val, "Stupa-Präsidium Universität Hamburg")
}

SELECT ?id WHERE { ?id a <urn:pwid:meg:e:Individual> . ?id <http://schema.org/name> "Noam Greenberg"^^<http://www.w3.org/2001/XMLSchema#string> . }
         */

        Variable idVariable = SparqlBuilder.var("id");
        Variable valVariable = SparqlBuilder.var("val");

        SelectQuery findDuplicates = Queries.SELECT(idVariable).where(
                idVariable.isA(this.valueFactory.createIRI(duplicate.type()))
                        .and(idVariable.has(duplicate.sharedProperty(),valVariable ))
                        .filter(Expressions.equals(Expressions.str(valVariable), Rdf.literalOf(duplicate.sharedValue)))
        );

        return this.queryServices.queryValues(findDuplicates, RepositoryType.ENTITIES, ctx)
                .doOnSubscribe(subscription -> log.trace("Retrieving all duplicates of same type with value '{}' for property '{}' ", duplicate.sharedValue, duplicate.sharedProperty))
                .flatMap(bindings -> {
                    Value id = bindings.getValue(idVariable.getVarName());

                    if (id.isIRI()) {
                        return Mono.just(new Duplicate((IRI) id));
                    } else return Mono.empty();

                });

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
    private Mono<Void> mergeDuplicates(List<Duplicate> duplicates, SessionContext ctx) {
        if (duplicates.isEmpty()) return Mono.empty();

        TreeSet<Duplicate> orderedDuplicates = new TreeSet<>(duplicates);
        Duplicate original = orderedDuplicates.first();
        SortedSet<Duplicate> deletionCandidates = orderedDuplicates.tailSet(original, false);


        /* relink */
        return Flux.fromIterable(deletionCandidates)
                .doOnSubscribe(subscription -> log.trace("Trying to merge all duplicates, keeping entity '{}' as original", original.id()))
                .flatMap(duplicate ->
                        this.findStatementsPointingToDuplicate(duplicate, ctx)
                                .flatMap(mislinkedStatement -> this.relinkEntity(mislinkedStatement.subject(), mislinkedStatement.predicate(), mislinkedStatement.object(), original.id(), ctx))
                                .doOnNext(trx -> log.info("Relinking completed in Transaction {}", trx.getIdentifier()))
                                .map(transaction -> duplicate)
                                .switchIfEmpty(Mono.just(duplicate))
                )
                .flatMap(duplicate ->
                        this.removeDuplicate(duplicate.id(), ctx)
                                .doOnNext(trx -> log.info("Removed duplicate entity with identifier '{}' in transaction '{}'", duplicate.id(), trx.getIdentifier()))
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
    private Flux<MislinkedStatement> findStatementsPointingToDuplicate(Duplicate duplicate, SessionContext ctx) {
        Variable subject = SparqlBuilder.var("s");
        Variable predicate = SparqlBuilder.var("p");

        SelectQuery query = Queries.SELECT(subject, predicate).where(subject.has(predicate, duplicate.id()));
        return queryServices.queryValues(query, RepositoryType.ENTITIES, ctx)
                .doOnSubscribe(subscription -> log.trace("Retrieving all statements pointing to duplicate with id '{}'", duplicate.id()))
                .flatMap(binding -> {
                    Value pVal = binding.getValue(predicate.getVarName());
                    Value sVal = binding.getValue(subject.getVarName());

                    if (pVal.isIRI() && sVal.isIRI()) {
                        log.trace("Statement with subject identifier {} pointing with  predicate {} to the duplicate", sVal.stringValue(), pVal.stringValue());
                        return Mono.just(new MislinkedStatement((IRI) sVal, (IRI) pVal, duplicate.id()));
                    } else return Mono.empty();
                });

    }

    private Flux<DuplicateCandidate> findCandidates(IRI sharedProperty, SessionContext ctx) {

        /*
        select ?type ?shared where {
          ?thing 	a ?type .
          ?thing 	<sharedProperty> ?value .
        } group by ?type ?id
        having (count(?thing) > 1)

         */

        Variable sharedValue = SparqlBuilder.var("s");
        Variable type = SparqlBuilder.var("type");
        Variable thing = SparqlBuilder.var("thing");

        SelectQuery findDuplicates = Queries.SELECT(type, sharedValue)
                .where(
                        thing.isA(type),
                        thing.has(sharedProperty, sharedValue)
                ).groupBy(sharedValue, type).having(Expressions.gt(Expressions.count(thing), 1)).limit(this.limit);


        return queryServices.queryValues(findDuplicates, RepositoryType.ENTITIES, ctx)
                .map(binding -> {
                    Value sharedValueVal = binding.getValue(sharedValue.getVarName());
                    Value typeVal = binding.getValue(type.getVarName());
                    return new DuplicateCandidate(sharedProperty, typeVal.stringValue(), sharedValueVal.stringValue());
                }).timeout(Duration.of(60, ChronoUnit.SECONDS));


    }

    private record DuplicateCandidate(IRI sharedProperty, String type, String sharedValue) {
    }

    private record MislinkedStatement(IRI subject, IRI predicate, IRI object) {
    }

    private record Duplicate(IRI id) implements Comparable<Duplicate> {
        @Override
        public int compareTo(Duplicate o) {
            return o.id().stringValue().compareTo(this.id().stringValue());
        }
    }


}


/*

## Original state

d20398wioe a ns1:VideoObject ;
    ns1:identifier "_a1234" ;
    ns1:title "Video 1" ;
	ns1:dateCreated "2022-12-12T12:32:24" .

90ildsm32i a ns1:VideoObject ;
    ns1:identifier "_a1234" ;
    ns1:title "Video 1" ;
	ns1:dateCreated "2022-12-12T13:32:24" .


dfm3209ews a ns1:VideoObject ;
    ns1:identifier "_a1234" ;
    ns1:title "Video 1" ;
	ns1:dateCreated "2022-12-12T13:32:26" .


## Desired state

d20398wioe a ns1:VideoObject ;
    ns1:identifier "_a1234" ;
    local:alternativeIdentifier "90ildsm32i", "dfm3209ews"
    ns1:title "Video 1" ;
	ns1:dateCreated "2022-12-12T12:32:24" .


## Response for GET /entities/d20398wioe

d20398wioe a ns1:VideoObject ;
    ns1:identifier "_a1234" ;
    ns1:title "Video 1" ;
	ns1:dateCreated "2022-12-12T12:32:24" .


## Response for GET /entities/90ildsm32i
    HTTP 302 GET /entities/d20398wioe

 */