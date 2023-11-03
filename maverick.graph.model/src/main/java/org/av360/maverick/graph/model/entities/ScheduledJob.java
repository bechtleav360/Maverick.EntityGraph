package org.av360.maverick.graph.model.entities;

import org.av360.maverick.graph.model.context.SessionContext;
import reactor.core.publisher.Mono;

public interface ScheduledJob {

    String getName();

    Mono<Void> run(SessionContext ctx);

}




