package org.av360.maverick.graph.services;

import org.av360.maverick.graph.model.context.SessionContext;
import reactor.core.publisher.Mono;

public interface SessionContextBuilder {
    Mono<SessionContext> build(SessionContext context);

}
