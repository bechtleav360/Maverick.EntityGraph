/*
 * Copyright (c) 2023.
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

package org.av360.maverick.graph.api.converter.services;

import org.av360.maverick.graph.api.config.ReactiveRequestUriContextHolder;
import org.av360.maverick.graph.model.context.RequestDetails;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.services.SessionContextBuilder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Fetches any custom header starting with "X-MEG-" and stores it in the session context
 */
@Component
public class CustomHeaderContextBuilder implements SessionContextBuilder  {
    @Override
    public Mono<SessionContext> build(SessionContext context) {
        return Mono.zip(
                ReactiveRequestUriContextHolder.getURI(),
                ReactiveRequestUriContextHolder.getHeaders()
        ).map(tuple -> {
            RequestDetails requestDetails = new RequestDetails();
            requestDetails.setHeaders(tuple.getT2().toSingleValueMap());
            requestDetails.setRequestURI(tuple.getT1());
            return context.withRequestDetails(requestDetails);

        }).switchIfEmpty(Mono.just(context));
    }
}
