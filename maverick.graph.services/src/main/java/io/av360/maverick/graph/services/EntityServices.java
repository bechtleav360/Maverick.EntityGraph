package io.av360.maverick.graph.services;

import io.av360.maverick.graph.store.rdf.models.Entity;
import io.av360.maverick.graph.store.rdf.models.Incoming;
import io.av360.maverick.graph.store.rdf.models.Transaction;
import org.eclipse.rdf4j.model.IRI;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface EntityServices {
    Mono<Entity> readEntity(String entityIdentifier, Authentication authentication);

    Mono<Transaction> deleteEntity(IRI entityIdentifier, Authentication authentication);

    Mono<Transaction> deleteEntity(String entityIdentifier, Authentication authentication);

    Mono<Transaction> createEntity(Incoming triples, Map<String, String> parameters, Authentication authentication);

    Mono<Transaction> linkEntityTo(String entityIdentifier, String predicatePrefix, String predicateKey, Incoming linkedEntities, Authentication authentication);
}
