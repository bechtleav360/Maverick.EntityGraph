package io.av360.maverick.graph.services;

import io.av360.maverick.graph.model.rdf.NamespaceAwareStatement;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface QueryServices {
    Flux<BindingSet> queryValues(String query, Authentication authentication);

    Flux<BindingSet> queryValues(SelectQuery query, Authentication authentication);

    Flux<NamespaceAwareStatement> queryGraph(String query, Authentication authentication);


    Mono<?> findEntityByProperty(String identifier, String prefix, String property, Authentication authentication);
}
