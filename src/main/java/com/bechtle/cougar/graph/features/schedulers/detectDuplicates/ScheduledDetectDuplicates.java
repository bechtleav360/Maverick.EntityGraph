package com.bechtle.cougar.graph.features.schedulers.detectDuplicates;


import com.bechtle.cougar.graph.api.security.AdminAuthentication;
import com.bechtle.cougar.graph.domain.services.EntityServices;
import com.bechtle.cougar.graph.domain.services.QueryServices;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
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

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Regular check for duplicates in the entity stores.
 *
 * A typical example for a duplicate are the following two entities uploaded on different times
 *
 * [] a ns1:VideoObject ;
 *     ns1:hasDefinedTerm [
 *          a ns1:DefinedTerm ;
 *          rdfs:label "Term 1"
 *      ] .
 *
 *
 * [] a ns1:VideoObject ;
 *     ns1:hasDefinedTerm [
 *          a ns1:DefinedTerm ;
 *          rdfs:label "Term 1"
 *      ] .
 *
 *  They both share the defined term "Term 1". Since they are uploaded in different requests, we don't check for duplicates. The (embedded) entity
 *
 *  x a DefinedTerm
 *    label "Term 1"
 *
 *  is therefore a duplicate in the repository after the second upload. This scheduler will check for these duplicates by looking at objects which
 *  - share the same label
 *  - share the same original_identifier
 *
 *
 *  TODO:
 *      For now we keep the duplicate but reroute all links to the original.
 */
@Component
@Slf4j(topic = "cougar.graph.schedulers.duplicates")
@ConditionalOnProperty(name = "application.features.schedulers.detectDuplicates", havingValue = "true")
public class ScheduledDetectDuplicates {


    private final EntityServices entityServices;
    private final QueryServices queryServices;
    private final SimpleValueFactory valueFactory;

    public ScheduledDetectDuplicates(EntityServices service, QueryServices queryServices) {
        this.entityServices = service;
        this.queryServices = queryServices;
        this.valueFactory = SimpleValueFactory.getInstance();
    }


    // https://github.com/spring-projects/spring-framework/issues/23533
    private boolean labelCheckRunning = false;

    @Scheduled(fixedRate = 10000)
    public void checkForDuplicatesScheduled() {
        if (labelCheckRunning) return;

        AdminAuthentication adminAuthentication = new AdminAuthentication();
        adminAuthentication.setAuthenticated(true);

        // FIXME: do this with all applicatations (run-as semantics)

        this.checkForDuplicates(adminAuthentication).publishOn(Schedulers.single()).subscribe();
    }

    public Mono<Void> checkForDuplicates(Authentication authentication) {
        return this.findCandidates(RDFS.LABEL, authentication)
                .map(duplicate -> {
                    log.trace("Found multiple entities in store with shared type '{}' and label '{}'", duplicate.type(), duplicate.sharedValue());
                    return duplicate;
                })
                .flatMap(duplicate -> this.findDuplicates(duplicate, authentication))
                .collectList()
                .map(list -> {
                    log.trace("Number of duplicates: {}", list.size());
                    return list;
                })
                .flatMapMany(duplicates -> this.mergeDuplicates(duplicates, authentication))
                .doOnSubscribe(sub -> {
                    log.trace("Checking duplicates sharing the same label");
                    labelCheckRunning = true;

                })
                .doOnComplete(() -> {
                    labelCheckRunning = false;
                }).thenEmpty(Mono.empty());

    }

    private Mono<Void> mergeDuplicateCandidates(List<DuplicateCandidate> duplicates) {
        if(duplicates.isEmpty()) return Mono.empty();

        TreeSet<DuplicateCandidate> orderedDuplicates = new TreeSet<>(duplicates);
        DuplicateCandidate original = orderedDuplicates.first();
        orderedDuplicates.tailSet(original).forEach(duplicate -> {

                    // collect duplicate identifiers and store in original
                    log.trace("duplicate id: {}", duplicate.sharedValue());

                    // save original

                    // delete duplicates

                }
        );

        return Mono.empty();

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
                .map(bindings -> {
                    Value id = bindings.getValue(idVariable.getVarName());
                    return new Duplicate(id);
                });

    }

    private Flux<Value> mergeDuplicates(List<Duplicate> duplicates, Authentication authentication) {
        if(duplicates.isEmpty()) return Flux.empty();

        TreeSet<Duplicate> orderedDuplicates = new TreeSet<>(duplicates);
        Duplicate original = orderedDuplicates.first();
        SortedSet<Duplicate> deletionCandidates = orderedDuplicates.tailSet(original, false);

        return Flux.fromIterable(deletionCandidates)
                .flatMap(duplicate -> this.findStatementsPointingToDuplicate(duplicate, authentication))
                .map(LostStatement::pVal);

        // save original

        // delete duplicates




    }

    private Flux<LostStatement> findStatementsPointingToDuplicate(Duplicate duplicate, Authentication authentication) {

        /*
        SELECT * WHERE {
          ?sub ?pre <duplicate id> .
        }
         */

        Variable subject = SparqlBuilder.var("s");
        Variable predicate = SparqlBuilder.var("p");

        SelectQuery query = Queries.SELECT(subject, predicate).where(subject.has(predicate, duplicate.id()));
        log.trace("XXXXX - {}",query.getQueryString());
        return queryServices.queryValues(query, authentication)
                .doOnSubscribe(subscription -> log.trace("Retrieving all statements pointing to duplicate with id '{}'", duplicate.id()))
                .map(binding -> {
                    Value pVal = binding.getValue(predicate.getVarName());
                    Value sVal = binding.getValue(subject.getVarName());
                    log.trace("lost statement with sub {} and pred {}", sVal.stringValue(), pVal.stringValue());
                    return new LostStatement(sVal, pVal);
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
        ).groupBy(sharedValue, type).having(Expressions.gt(Expressions.count(thing), 1));


        return queryServices.queryValues(findDuplicates, authentication)
                .map(binding -> {
                    Value sharedValueVal = binding.getValue(sharedValue.getVarName());
                    Value typeVal = binding.getValue(type.getVarName());
                    return new DuplicateCandidate(sharedProperty, typeVal.stringValue(), sharedValueVal.stringValue());
                });

    }


    private record DuplicateCandidate(IRI sharedProperty, String type, String sharedValue) {}


    private record LostStatement(Value sVal, Value pVal) {}

    private record Duplicate(Value id) implements Comparable<Duplicate> {
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