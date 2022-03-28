package com.bechtle.eagl.graph.domain.services.scheduler;


import com.bechtle.eagl.graph.domain.model.vocabulary.SDO;
import com.bechtle.eagl.graph.repository.rdf4j.repository.EntityRepository;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.TreeSet;

@Component
@Slf4j
@ConditionalOnProperty(name = "application.features.schedulers.detectDuplicates", havingValue = "true")
public class ScheduledDetectDuplicates {


    private final EntityRepository repository;
    private final SimpleValueFactory valueFactory;

    public ScheduledDetectDuplicates(EntityRepository repository) {
        this.repository = repository;
        this.valueFactory = SimpleValueFactory.getInstance();
    }

    @Scheduled(fixedDelay = 1000)
    public void checkForDuplicates() {
        log.info("Duplicate check started");

        this.findCandidates()
                .map(duplicate -> {
                    log.info("Duplicated detected with type {} and id {}", duplicate.type(), duplicate.originalIdentifier());
                    return duplicate;
                })
                .map(this::findDuplicates)
                .subscribe();
    }

    private Flux<DuplicateCandidate> findDuplicates(DuplicateCandidate duplicate) {

        /*
        select ?thing ?date where {
  ?thing 	a <http://schema.org/video>.
  ?thing 	<http://schema.org/identifier> "_a1238" .
    ?thing 	<http://schema.org/dateCreated> ?date .
}
         */
        Variable dateVariable = SparqlBuilder.var("date");
        Variable idVariable = SparqlBuilder.var("identifier");

        SelectQuery findDuplicates = Queries.SELECT(idVariable, dateVariable).where(
                idVariable.isA(this.valueFactory.createIRI(duplicate.type())),
                idVariable.has(SDO.IDENTIFIER, this.valueFactory.createLiteral(duplicate.originalIdentifier()))
        );
        this.repository.select(findDuplicates)
                .map(tupleQueryResult -> tupleQueryResult.stream().map(bindings -> {
                        Value date = bindings.getValue("date");
                        Value id = bindings.getValue("identifier");
                        return new Duplicate(id.stringValue(), date.stringValue());
                        }).toList()
                ).map(this::mergeDuplicates); 





        return Flux.just(duplicate);
    }

    private Mono<Void> mergeDuplicates(List<Duplicate> duplicates) {
        TreeSet<Duplicate> orderedDuplicates = new TreeSet<>(duplicates);
        Duplicate original = orderedDuplicates.first();
        orderedDuplicates.tailSet(original).stream().forEach(duplicate ->  {

                // collect duplicate identifiers and store in original

                // save original

                // delete duplicates

                }
        );

        return Mono.empty();



    }

    private Flux<DuplicateCandidate> findCandidates() {

        /*
        select ?type ?id where {
  ?thing 	a ?type .
  ?thing 	<http://schema.org/identifier> ?id .
} group by ?type ?id
having (count(?thing) > 1)

         */

        Variable id = SparqlBuilder.var("identifier");
        Variable type = SparqlBuilder.var("type");
        Variable thing = SparqlBuilder.var("thing");

        SelectQuery findDuplicates = Queries.SELECT(type, id).where(
                thing.isA(type),
                thing.has(SDO.IDENTIFIER, id)
        ).groupBy(id, type).having(Expressions.gt(Expressions.count(thing), 1));



        Mono<TupleQueryResult> result = repository.select(findDuplicates);
        return result.flatMapMany(res ->
                Flux.<DuplicateCandidate>create(c -> {
            res.forEach(bindings -> {
                Value identifier = bindings.getValue("identifier");
                Value type1 = bindings.getValue("type");
                c.next(new DuplicateCandidate(type1.stringValue(), identifier.stringValue()));
            });
            c.complete();
        }));


    }


    private record DuplicateCandidate(String type, String originalIdentifier) {   }
    private record Duplicate(String id, String creationDate) implements Comparable<Duplicate> {
        @Override
        public int compareTo(Duplicate o) {
            return o.creationDate().compareTo(this.creationDate());
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