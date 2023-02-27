package io.av360.maverick.graph.services.schedulers.detectDuplicates;


import io.av360.maverick.graph.model.security.ApiKeyAuthenticationToken;
import io.av360.maverick.graph.model.security.Authorities;
import io.av360.maverick.graph.model.vocabulary.SDO;
import io.av360.maverick.graph.services.EntityServices;
import io.av360.maverick.graph.services.QueryServices;
import io.av360.maverick.graph.services.ValueServices;
import io.av360.maverick.graph.store.rdf.models.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
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
@Component
@Slf4j(topic = "graph.schedulers.duplicates")
@ConditionalOnProperty(name = "application.features.schedulers.detectDuplicates", havingValue = "true")
public class ScheduledDetectDuplicates {

    private record DuplicateCandidate(IRI sharedProperty, String type, String sharedValue) {    }


    private record MislinkedStatement(Resource subject, IRI predicate, IRI object) {     }

    private record Duplicate(IRI id) implements Comparable<Duplicate> {
        @Override
        public int compareTo(Duplicate o) {
            return o.id().stringValue().compareTo(this.id().stringValue());
        }
    }

    private final EntityServices entityServices;
    private final QueryServices queryServices;

    private final ValueServices valueServices;
    private final SimpleValueFactory valueFactory;

    public ScheduledDetectDuplicates(EntityServices service, QueryServices queryServices, ValueServices valueServices) {
        this.entityServices = service;
        this.queryServices = queryServices;
        this.valueServices = valueServices;
        this.valueFactory = SimpleValueFactory.getInstance();
    }


    // https://github.com/spring-projects/spring-framework/issues/23533
    private boolean labelCheckRunning = false;

    @Scheduled(fixedRate = 1000)
    public void checkForDuplicatesScheduled() {
        if (labelCheckRunning) return;

        ApiKeyAuthenticationToken adminAuthentication = new ApiKeyAuthenticationToken(new HashMap<>());
        adminAuthentication.setAuthenticated(true);
        adminAuthentication.grantAuthority(Authorities.SYSTEM);

        // FIXME: do this with all applicatations (run-as semantics)


        this.checkForDuplicates(RDFS.LABEL, adminAuthentication)
                .then(this.checkForDuplicates(SDO.IDENTIFIER, adminAuthentication))
                .then(this.checkForDuplicates(SKOS.PREF_LABEL, adminAuthentication))
                .then(this.checkForDuplicates(DCTERMS.IDENTIFIER, adminAuthentication))
                .publishOn(Schedulers.single()).subscribe();
    }

    public Mono<Void> checkForDuplicates(IRI characteristicProperty, Authentication authentication) {
        return this.findCandidates(characteristicProperty, authentication)
                .map(candidate -> {
                    log.trace("There are multiple entities with shared type '{}' and label '{}'", candidate.type(), candidate.sharedValue());
                    return candidate;
                })
                .flatMap(candidate ->
                        this.findDuplicates(candidate, authentication)
                                .doOnNext(duplicate -> log.trace("Entity '{}' identified as duplicate. Another item exists with property {} and value {} .", duplicate.id(), candidate.sharedProperty(), candidate.sharedValue()))
                                .collectList()
                                .flatMap(duplicates -> this.mergeDuplicates(duplicates, authentication)))
                .doOnSubscribe(sub -> {
                    log.trace("Checking duplicates sharing the same label");
                    labelCheckRunning = true;

                })
                .doOnComplete(() -> {
                    labelCheckRunning = false;
                }).thenEmpty(Mono.empty());

    }


    private Flux<Duplicate> findDuplicates(DuplicateCandidate duplicate, Authentication authentication) {

        /*
        select ?thing where {
                ?thing 	a <http://schema.org/video>.
  ?thing 	<http://schema.org/identifier> "_a1238" .
    ?thing 	<http://schema.org/dateCreated> ?date .
}
         */
        Variable idVariable = SparqlBuilder.var("id");

        SelectQuery findDuplicates = Queries.SELECT(idVariable).where(
                idVariable.isA(this.valueFactory.createIRI(duplicate.type())),
                idVariable.has(duplicate.sharedProperty(), this.valueFactory.createLiteral(duplicate.sharedValue()))
        );

        return this.queryServices.queryValues(findDuplicates, authentication)
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
     * @param authentication
     * @return
     */
    private Mono<Void> mergeDuplicates(List<Duplicate> duplicates, Authentication authentication) {
        if (duplicates.isEmpty()) return Mono.empty();

        TreeSet<Duplicate> orderedDuplicates = new TreeSet<>(duplicates);
        Duplicate original = orderedDuplicates.first();
        SortedSet<Duplicate> deletionCandidates = orderedDuplicates.tailSet(original, false);


        /* relink */
        return Flux.fromIterable(deletionCandidates)
                .doOnSubscribe(subscription -> log.trace("Trying to merge all duplicates, keeping entity '{}' as original", original.id()))
                .flatMap(duplicate ->
                        this.findStatementsPointingToDuplicate(duplicate, authentication)
                                .flatMap(mislinkedStatement -> this.relinkEntity(mislinkedStatement.subject(), mislinkedStatement.predicate(), mislinkedStatement.object(), original.id(), authentication))
                                .doOnNext(trx -> log.info("Relinking completed in Transaction {}", trx.getIdentifier()))
                                .map(transaction -> duplicate)
                )
                .flatMap(duplicate ->
                        this.removeDuplicate(duplicate.id(), authentication)
                                .doOnNext(trx -> log.info("Removed duplicate entity with identifier '{}' in transaction '{}'", duplicate.id(), trx.getIdentifier()))
                )
                .then();
    }


    private Mono<Transaction> removeDuplicate(IRI object, Authentication authentication) {
        return this.entityServices.deleteEntity(object, authentication);
    }

    private Mono<Transaction> relinkEntity(Resource subject, IRI predicate, Value object, Value id, Authentication authentication) {
        return this.valueServices.replaceValue(subject, predicate, object, id, authentication);
    }


    /**
     * Runs the query:   SELECT * WHERE { ?sub ?pre <duplicate id> }. to find all entities pointing to the identified duplicate entity (with the goal to reroute those statements to the original)
     *
     * @param duplicate,      the identified duplicate
     * @param authentication, auth info
     * @return the statements pointing to the duplicate (as Flux)
     */
    private Flux<MislinkedStatement> findStatementsPointingToDuplicate(Duplicate duplicate, Authentication authentication) {
        Variable subject = SparqlBuilder.var("s");
        Variable predicate = SparqlBuilder.var("p");

        SelectQuery query = Queries.SELECT(subject, predicate).where(subject.has(predicate, duplicate.id()));
        return queryServices.queryValues(query, authentication)
                .doOnSubscribe(subscription -> log.trace("Retrieving all statements pointing to duplicate with id '{}'", duplicate.id()))
                .flatMap(binding -> {
                    Value pVal = binding.getValue(predicate.getVarName());
                    Value sVal = binding.getValue(subject.getVarName());

                    if (pVal.isIRI() && sVal.isResource()) {
                        log.trace("Statement with subject identifier {} pointing with  predicate {} to the duplicate", sVal.stringValue(), pVal.stringValue());
                        return Mono.just(new MislinkedStatement((Resource) sVal, (IRI) pVal, duplicate.id()));
                    } else return Mono.empty();
                });

    }

    private Flux<DuplicateCandidate> findCandidates(IRI sharedProperty, Authentication authentication) {

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
                ).groupBy(sharedValue, type).having(Expressions.gt(Expressions.count(thing), 1)).limit(100);


        String q = findDuplicates.getQueryString();

        return queryServices.queryValues(findDuplicates, authentication)
                .map(binding -> {
                    Value sharedValueVal = binding.getValue(sharedValue.getVarName());
                    Value typeVal = binding.getValue(type.getVarName());
                    return new DuplicateCandidate(sharedProperty, typeVal.stringValue(), sharedValueVal.stringValue());
                });

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