package org.av360.maverick.graph.services;

import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Flux;

public interface NavigationServices {
    public static String ResolvableUrlPrefix = "http://??";

    Flux<AnnotatedStatement> start(Authentication authentication, WebSession session);

    Flux<AnnotatedStatement> browse(MultiValueMap<String, String> params, Authentication authentication, WebSession session);
}
