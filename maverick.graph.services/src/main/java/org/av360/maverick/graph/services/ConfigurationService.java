package org.av360.maverick.graph.services;

import org.av360.maverick.graph.model.context.SessionContext;
import reactor.core.publisher.Mono;

public interface ConfigurationService {
    Mono<String> getValue(String key, SessionContext ctx);
}
