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

package org.av360.maverick.graph.services.api.details;

import org.av360.maverick.graph.services.api.Api;
import org.av360.maverick.graph.services.api.details.capabilities.InsertDetails;
import org.av360.maverick.graph.services.api.details.capabilities.ReadDetails;
import org.av360.maverick.graph.services.api.details.capabilities.RemoveDetails;

public class DetailsApi {
    private final Api parent;
    private final RemoveDetails remove;
    private final InsertDetails insert;
    private final ReadDetails read;

    public DetailsApi(Api parent) {
        this.parent = parent;

        insert = new InsertDetails(this.parent);
        remove = new RemoveDetails(this.parent);
        read = new ReadDetails((this.parent));
    }

    public RemoveDetails removes() {
        return remove;
    }

    public InsertDetails inserts() {
        return insert;
    }

    public ReadDetails selects() {
        return read;
    }
}