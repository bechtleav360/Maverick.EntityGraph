package org.av360.maverick.graph.services;

import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.eclipse.rdf4j.model.IRI;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Flux;

import java.util.Map;

public interface NavigationServices {
    public static String ResolvableUrlPrefix = "http://??";

    Flux<AnnotatedStatement> start(Authentication authentication);

    Flux<AnnotatedStatement> browse(Map<String, String> params, Authentication authentication);


    IRI generateResolvableIRI(String path, Map<String, String> params);

    default IRI generateResolvableIRI(String path) {
        return this.generateResolvableIRI(path, Map.of());
    }
}
