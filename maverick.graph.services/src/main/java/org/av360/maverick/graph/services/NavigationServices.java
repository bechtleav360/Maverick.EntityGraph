package org.av360.maverick.graph.services;

import jakarta.annotation.Nullable;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.eclipse.rdf4j.model.IRI;
import reactor.core.publisher.Flux;

import java.util.Map;

public interface NavigationServices {
    public static String ResolvableUrlPrefix = "http://??";

    Flux<AnnotatedStatement> start(SessionContext ctx);

    Flux<AnnotatedStatement> list(Map<String, String> requestParams, SessionContext ctx, @Nullable String query);

    Flux<AnnotatedStatement> browse(Map<String, String> params, SessionContext ctx);


    IRI generateResolvableIRI(String path, Map<String, String> params);

    default IRI generateResolvableIRI(String path) {
        return this.generateResolvableIRI(path, Map.of());
    }
}
