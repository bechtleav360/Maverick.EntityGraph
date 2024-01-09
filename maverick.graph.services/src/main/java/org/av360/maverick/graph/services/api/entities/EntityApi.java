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

package org.av360.maverick.graph.services.api.entities;

import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.services.api.Api;
import org.av360.maverick.graph.services.api.entities.capabilities.*;
import org.av360.maverick.graph.store.IndividualsStore;
import reactor.core.publisher.Mono;

public class EntityApi {
    private final Api parent;
    public DeleteEntities delete() {
        return deleteEntities;
    }

    public InsertEntities insert() {
        return insertEntities;
    }

    public ReadEntities select() {
        return readEntities;
    }

    public UpdateEntities updates() {
        return updateEntities;
    }

    private final FindEntities findEntities; 
    private final DeleteEntities deleteEntities;
    private final InsertEntities insertEntities;
    private final ReadEntities readEntities;
    private final UpdateEntities updateEntities;
    public final IndividualsStore individualsStore;


    public EntityApi(Api parent, IndividualsStore individualsStore) {
        this.parent = parent;
        readEntities = new ReadEntities(this.parent, individualsStore);
        this.individualsStore = individualsStore;
        insertEntities = new InsertEntities(this.parent, individualsStore);
        deleteEntities = new DeleteEntities(this.parent, individualsStore);
        updateEntities = new UpdateEntities(this.parent, individualsStore);
        findEntities = new FindEntities(parent, individualsStore);
    }


    public Mono<Transaction> commit(Transaction trx, Environment environment) {
        return this.individualsStore.asCommitable().commit(trx, environment);
    }


    public IndividualsStore getStore() {
        return this.individualsStore;
    }

    public FindEntities find() {
        return findEntities;
    }
}