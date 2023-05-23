package org.av360.maverick.graph.model.entities;

import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

import java.io.Serializable;

public interface Job extends Serializable {
    String getName();

    Mono<Void> run(Authentication authentication);


}

