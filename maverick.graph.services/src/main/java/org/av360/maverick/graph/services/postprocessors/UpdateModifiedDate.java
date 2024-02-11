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

import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.events.EntityUpdatedEvent;
import org.av360.maverick.graph.store.IndividualsStore;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Component
public class UpdateModifiedDate {
    private final IndividualsStore entityStore;

    public UpdateModifiedDate(IndividualsStore entityStore) {
        this.entityStore = entityStore;
    }

    @Async
    @EventListener
    void handleEntitiesCreated(EntityUpdatedEvent event) {
        Flux.fromIterable(event.listUpdatedEntityIdentifiers())
                .flatMap(iri -> handleEntityUpdated(iri, event.getEnvironment()))
                .subscribe();
    }

    Mono<Void> handleEntityUpdated(Resource entityIdentifier, Environment environment) {
        return this.entityStore.asFragmentable().getFragment(entityIdentifier, environment)
                .filter(rdfFragment -> rdfFragment.isIndividual() || rdfFragment.isClassifier())
                .flatMap(rdfFragment -> {
                    RdfTransaction trx = new RdfTransaction();

                    // remove old modified date statements
                    rdfFragment.streamStatements(rdfFragment.getIdentifier(), DCTERMS.MODIFIED, null)
                            .forEach(trx::removes);

                    // insert current date as last modification date
                    trx.inserts(
                            rdfFragment.getIdentifier(),
                            DCTERMS.CREATED,
                            Values.literal(ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                    );

                    return this.entityStore.asCommitable().commit(
                            trx,
                            environment
                    );
                }).then();

    }
}
