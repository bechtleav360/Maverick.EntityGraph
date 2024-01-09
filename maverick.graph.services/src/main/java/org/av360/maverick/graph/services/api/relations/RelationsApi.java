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

package org.av360.maverick.graph.services.api.relations;

import org.av360.maverick.graph.services.api.Api;
import org.av360.maverick.graph.services.api.relations.capabilities.ReadRelations;
import org.av360.maverick.graph.services.api.relations.capabilities.UpdateRelations;

public class RelationsApi {
    private final Api api;
    private final UpdateRelations updates;
    private final ReadRelations selects;


    public RelationsApi(Api api) {

        this.api = api;
        this.updates = new UpdateRelations(api);
        this.selects = new ReadRelations(api);

    }

    public UpdateRelations updates() {
        return this.updates;
    }

    public ReadRelations selects() {
        return this.selects;
    }
}
