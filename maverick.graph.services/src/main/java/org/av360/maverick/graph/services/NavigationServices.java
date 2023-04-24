package org.av360.maverick.graph.services;

import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import reactor.core.publisher.Flux;

public interface NavigationServices {

    Flux<AnnotatedStatement> start();
}
