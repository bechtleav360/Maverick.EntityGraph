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

package org.av360.maverick.graph.services.api.values;


import org.av360.maverick.graph.services.api.Api;
import org.av360.maverick.graph.services.api.values.capabilities.DeleteValue;
import org.av360.maverick.graph.services.api.values.capabilities.InsertValues;
import org.av360.maverick.graph.services.api.values.capabilities.ReadValues;

public class ValuesApi {
    private final Api parent;

    public InsertValues insert() {
        return insertValues;
    }

    public DeleteValue remove() {
        return removeValues;
    }

    public ReadValues read() {
        return readValues;
    }



    public final InsertValues insertValues;
    public final DeleteValue removeValues;
    public final ReadValues readValues;

    public ValuesApi(Api parent) {
        this.parent = parent;
        insertValues = new InsertValues(this.parent);
        removeValues = new DeleteValue((this.parent));
        readValues = new ReadValues((this.parent));
    }


}