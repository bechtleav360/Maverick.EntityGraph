package org.av360.maverick.graph.model.entities;

import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

public interface Job {
    String getName();

    Mono<Void> run(Authentication authentication);
}

