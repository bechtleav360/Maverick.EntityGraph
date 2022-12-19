package io.av360.maverick.graph.services;

import io.av360.maverick.graph.store.rdf.models.Entity;
import io.av360.maverick.graph.store.rdf.models.TripleBag;
import io.av360.maverick.graph.store.rdf.models.Transaction;
import org.eclipse.rdf4j.model.IRI;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface EntityServices {

    /**
     * Retrieves a complete entity representation (identifier, values and relations) from store.
     *
     * @param entityIdentifier The unique entity identifier
     * @param authentication   The current authentication
     * @return Entity as Mono
     */
    Mono<Entity> readEntity(String entityIdentifier, Authentication authentication);

    /**
     * Deletes an entity with all its values from the store.
     *
     * @param entityIdentifier The unique entity identifier as IRI
     * @param authentication   The current authentication
     * @return Transaction with affected statements
     */
    Mono<Transaction> deleteEntity(IRI entityIdentifier, Authentication authentication);

    /**
     * Deletes an entity with all its values from the store.
     *
     * @param entityIdentifier The unique entity identifier as String
     * @param authentication   The current authentication
     * @return Transaction with affected statements
     */
    Mono<Transaction> deleteEntity(String entityIdentifier, Authentication authentication);

    /**
     * Creates entities from the incoming set of triples
     *
     * @param triples        A Set of triples
     * @param parameters     Additional parameters coming through the request.
     * @param authentication The current authentication
     * @return Transaction with affected statements
     */
    Mono<Transaction> createEntity(TripleBag triples, Map<String, String> parameters, Authentication authentication);

    Mono<Transaction> linkEntityTo(String entityIdentifier, String predicatePrefix, String predicateKey, TripleBag linkedEntities, Authentication authentication);
}
