package org.av360.maverick.graph.services;

import jakarta.annotation.Nullable;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.model.vocabulary.Local;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.util.Values;
import reactor.core.publisher.Flux;

import java.util.Map;

public interface NavigationServices {
    public static String ResolvableUrlPrefix = "urn:pwid:meg:nav:";

    IRI NAVIGATION_CONTEXT = Values.iri(Local.URN_PREFIX+"nav");
    IRI DATA_CONTEXT = Values.iri(Local.URN_PREFIX+"data");
    IRI DETAILS_CONTEXT = Values.iri(Local.URN_PREFIX+"details");


    Flux<AnnotatedStatement> start(SessionContext ctx);

    Flux<AnnotatedStatement> list(Map<String, String> requestParams, SessionContext ctx, @Nullable String query);


    Flux<AnnotatedStatement> view(Map<String, String> requestParams, SessionContext ctx);

    Flux<AnnotatedStatement> browse(Map<String, String> params, SessionContext ctx);


    Literal generateResolvableIRI(String path, Map<String, String> params);

    default Literal generateResolvableIRI(String path) {
        return this.generateResolvableIRI(path, Map.of());
    }
}
