package org.av360.maverick.graph.services;

import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;

public interface NavigationServices {

    Flux<AnnotatedStatement> start(Authentication authentication);

    Flux<AnnotatedStatement> browse(MultiValueMap<String, String> params, Authentication authentication);
}
