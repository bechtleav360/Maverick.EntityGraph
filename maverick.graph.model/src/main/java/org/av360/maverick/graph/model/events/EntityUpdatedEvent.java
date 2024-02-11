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

package org.av360.maverick.graph.model.events;

import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.entities.Transaction;
import org.eclipse.rdf4j.model.Resource;

import java.util.HashSet;
import java.util.Set;

public abstract class EntityUpdatedEvent extends EntityEvent {

    public EntityUpdatedEvent(Transaction source, Environment environment) {
        super(source, environment);
    }

    public Set<Resource> listUpdatedEntityIdentifiers() {
        Set<Resource> result = new HashSet<>();
        result.addAll(super.getTransaction().getInsertedStatements().filter(null, null, null).subjects());
        result.addAll(super.getTransaction().getInsertedStatements().filter(null, null, null).subjects());
        return result;
    }
}
