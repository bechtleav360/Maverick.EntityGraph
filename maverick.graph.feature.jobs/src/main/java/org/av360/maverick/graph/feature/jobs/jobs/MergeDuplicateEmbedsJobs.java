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

package org.av360.maverick.graph.feature.jobs.jobs;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.annotations.Job;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.entities.ScheduledJob;
import reactor.core.publisher.Mono;

@Job
@Slf4j(topic = "graph.jobs.duplicateEmbeds")
public class MergeDuplicateEmbedsJobs implements ScheduledJob {

    public static String NAME = "detectDuplicateEmbeds";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Mono<Void> run(SessionContext ctx) {
        return Mono.empty();
    }
}
