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

package org.av360.maverick.graph.services.api;


import lombok.Getter;
import org.av360.maverick.graph.services.IdentifierServices;
import org.av360.maverick.graph.services.api.details.DetailsApi;
import org.av360.maverick.graph.services.api.entities.EntityApi;
import org.av360.maverick.graph.services.api.identifiers.IdentifiersApi;
import org.av360.maverick.graph.services.api.relations.RelationsApi;
import org.av360.maverick.graph.services.api.schema.SchemaApi;
import org.av360.maverick.graph.services.api.values.ValuesApi;
import org.av360.maverick.graph.store.IndividualsStore;
import org.av360.maverick.graph.store.SchemaStore;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@Getter
public class Api {



    final ApplicationEventPublisher eventPublisher;

    private final IdentifierServices identifierServices;

    private final SchemaStore schemaStore;
    private final IndividualsStore entityStore;

    private final SchemaApi schemaApi;
    private final IdentifiersApi identifiersApi;
    private final EntityApi entitiesApi;
    private final DetailsApi detailsApi;
    private final ValuesApi valuesApi;
    private final RelationsApi relationsApi;

    public Api(ApplicationEventPublisher eventPublisher, IdentifierServices identifierServices, SchemaStore schemaStore, IndividualsStore entityStore) {
        this.eventPublisher = eventPublisher;
        this.identifierServices = identifierServices;
        this.schemaStore = schemaStore;
        this.entityStore = entityStore;


        relationsApi = new RelationsApi(this);
        valuesApi = new ValuesApi(this);
        detailsApi = new DetailsApi(this);
        schemaApi = new SchemaApi(this, schemaStore);
        identifiersApi = new IdentifiersApi(this, identifierServices);
        entitiesApi = new EntityApi(this, entityStore);
    }
    public DetailsApi details() { return detailsApi;}
    public IdentifiersApi identifiers() {
        return identifiersApi;
    }
    public EntityApi entities() {
        return entitiesApi;
    }
    public SchemaApi schema() {
        return schemaApi;
    }
    public ValuesApi values() {
        return valuesApi;
    }

    public RelationsApi relations() { return relationsApi; }
    public void publishEvent(ApplicationEvent applicationEvent) {
        this.eventPublisher.publishEvent(applicationEvent);
    }

}
