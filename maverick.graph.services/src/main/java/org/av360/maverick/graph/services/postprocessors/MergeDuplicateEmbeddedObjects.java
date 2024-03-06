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

package org.av360.maverick.graph.services.postprocessors;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.model.events.EntityCreatedEvent;
import org.av360.maverick.graph.model.events.EntityUpdatedEvent;
import org.av360.maverick.graph.model.vocabulary.meg.Local;
import org.av360.maverick.graph.store.IndividualsStore;
import org.av360.maverick.graph.store.rdf.fragments.RdfFragment;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class MergeDuplicateEmbeddedObjects {

    private final IndividualsStore entityStore;

    public MergeDuplicateEmbeddedObjects(IndividualsStore entityStore) {
        this.entityStore = entityStore;
    }

    @Async
    @EventListener
    void handleEntitiesUpdatedForDuplicateMerge(EntityUpdatedEvent event) {
        Flux.fromIterable(event.listUpdatedEntityIdentifiers())
                .flatMap(iri -> handleEntityCreated(iri, event.getEnvironment()))
                .doOnSubscribe(subscription -> log.debug("Postprocessing: Merge duplicate embedded objects"))
                .subscribe();
    }

    @Async
    @EventListener
    void handleEntitiesCreatedForDuplicateMerge(EntityCreatedEvent event) {
        Flux.fromIterable(event.listInsertedFragmentSubjects())
                .flatMap(iri -> handleEntityCreated(iri, event.getEnvironment()))
                .doOnSubscribe(subscription -> log.debug("Postprocessing: Merge duplicate embedded objects"))
                .subscribe();
    }


    /* collects the fragment for the created entity from store and forwards it */
    Mono<Void> handleEntityCreated(Resource entityIdentifier, Environment environment) {
        return this.entityStore.asFragmentable().getFragment(entityIdentifier, environment)
                .filter(rdfFragment -> rdfFragment.isIndividual() || rdfFragment.isClassifier())
                .flatMap(rdfFragment -> handleFragment(entityIdentifier, rdfFragment, environment))
                .then();

    }

    /* Fetches all embedded objects in the current fragment and forwards it */
    private Mono<Void> handleFragment(Resource entityIdentifier, RdfFragment rdfFragment, Environment environment) {
        return Flux.fromStream(rdfFragment.streamStatements(entityIdentifier, null, null))
                .filter(statement -> statement.getObject().isIRI() && rdfFragment.hasStatement((IRI) statement.getObject(), RDF.TYPE, Local.Entities.TYPE_EMBEDDED))
                .map(statement -> (IRI) statement.getObject())
                .map(embedIri -> {
                            Map<IRI, Set<ComparableValue>> values = rdfFragment.listStatements(embedIri, null, null)
                                    .stream()
                                    .collect(Collectors.groupingBy(Statement::getPredicate,
                                            Collectors.mapping(
                                                    statement -> new ComparableValue(statement.getObject()),
                                                    Collectors.toSet())));
                            return Pair.of(embedIri, values);
                        }

                )
                .collectList()
                .flatMap(list -> findDuplicates(list, entityIdentifier, rdfFragment, environment))
                .then();
        //.flatMap(embedsIdentifiers -> )
    }

    /* identifies duplicate embeds in the list and forwards them
    * Pair<IRI, Map<IRI, List<Value>>> -> Pair< Subject, Map< Predicate, List<Value>>>
    * */
    private Mono<Void> findDuplicates(List<Pair<IRI, Map<IRI, Set<ComparableValue>>>> pairs, Resource entityIdentifier, RdfFragment rdfFragment, Environment environment) {
        Set<IRI> duplicates = new HashSet<>();
        Set<IRI> locked = new HashSet<>();
        pairs.forEach(pair -> {
            pairs.forEach(candidate -> {
                if(pair.getKey().equals(candidate.getKey())) return;

                if(isEqual(candidate.getValue(), pair.getValue())) {
                    if(! locked.contains(candidate.getKey())) {
                        duplicates.add(candidate.getKey());
                        locked.add(pair.getKey());
                    }
                }
            });
        });

        if(duplicates.isEmpty()) {
            return Mono.empty();
        } else {
            return this.deleteDuplicates(duplicates, entityIdentifier, rdfFragment, environment);
        }
    }

    private boolean isEqual(Map<IRI, Set<ComparableValue>> left, Map<IRI, Set<ComparableValue>> right) {
        if (left.size() != right.size()) {
            return false;
        }
        if (! left.keySet().equals(right.keySet())) {
            return false;
        }

        boolean equal = true;
        for (Map.Entry<IRI, Set<ComparableValue>> predicateValue : left.entrySet()) {

            Set<ComparableValue> leftValuesForPredicate = predicateValue.getValue();
            Set<ComparableValue> rightValuesForPredicate = right.get(predicateValue.getKey());

            if (rightValuesForPredicate == null) {
                return false;
            }

            equal &= areListsEqual(leftValuesForPredicate, rightValuesForPredicate);

        }

        return equal;
    }

    private boolean areListsEqual(Set<ComparableValue> leftValues, Set<ComparableValue> rightValues) {
        return leftValues.equals(rightValues);
    }

    /* collects all statements from the fragment where the iris are the subject and deletes them */
    private Mono<Void> deleteDuplicates(Set<IRI> duplicates, Resource entityIdentifier, RdfFragment rdfFragment, Environment environment) {
        List<Statement> statementsToRemove = duplicates.stream()
                .flatMap(iri -> rdfFragment.listStatements(iri, null, null).stream())
                .toList();
        List<Statement> linkingStatementsToRemove = duplicates.stream()
                .flatMap(iri -> rdfFragment.listStatements(entityIdentifier, null, iri).stream())
                .toList();

        Transaction trx = new RdfTransaction()
                .removes(statementsToRemove)
                .removes(linkingStatementsToRemove);

        return this.entityStore.asCommitable().commit(trx, environment)
                .then();
    }

    private class ComparableValue {
        private final Value value;

        public ComparableValue(Value value) {
            this.value = value;
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ComparableValue ov)) return false;

            if(value.getClass() != ov.value.getClass()) return false;
            if(value.isIRI() || value.isBNode() || value.isTriple() || value.isResource()) {
                return value.stringValue().equals(ov.value.stringValue());
            }

            Literal left = (Literal) value;
            Literal right = (Literal) ov.value;

            if(! left.getDatatype().equals(XSD.STRING) && ! right.getDatatype().equals(XSD.STRING) && ! left.getDatatype().equals(right.getDatatype())) {
                return false;
            }

            if(left.getDatatype().equals(XSD.DATETIME) && areWithinTheSameHour(left.calendarValue(), right.calendarValue())) {
                return true;
            }

            boolean string_equals = left.stringValue().equalsIgnoreCase(right.stringValue());
            return string_equals;
        }

        public boolean areWithinTheSameHour(XMLGregorianCalendar cal1, XMLGregorianCalendar cal2) {
            // Compare year, month, day, and hour for equality
            return cal1.getYear() == cal2.getYear() &&
                    cal1.getMonth() == cal2.getMonth() &&
                    cal1.getDay() == cal2.getDay() &&
                    cal1.getHour() == cal2.getHour();
        }

        @Override
        public int hashCode() {
            // Hash code must be consistent with equals. Use the same fields and a similar logic.
            if (value != null) {
                return Objects.hash(value.stringValue());
            }
            return 0;
        }
    }
}
