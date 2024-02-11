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
import org.av360.maverick.graph.model.events.EntityCreatedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class InjectCreationDate implements ApplicationListener<EntityCreatedEvent> {
    @Override
    public void onApplicationEvent(EntityCreatedEvent event) {
        log.info("An entity has been created");
    }
}
