package io.av360.maverick.graph.services;

import io.av360.maverick.graph.store.EntityStore;
import io.av360.maverick.graph.store.rdf.models.Entity;
import io.av360.maverick.graph.store.rdf.models.Transaction;
import io.av360.maverick.graph.store.rdf.models.TripleBag;
import jakarta.annotation.Nullable;
import org.eclipse.rdf4j.model.IRI;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface EntityServices {


    /**
     * Retrieves a complete entity representation (identifier, values and relations) from store.
     *
     * @param entityIri The unique entity URI
     * @param authentication   The current authentication
     * @param neighbourLevel how many levels of neigbours to include (0 is entity only, 1 is direct neighbours)
     * @return Entity as Mono
     */
    Mono<Entity> get(IRI entityIri, Authentication authentication, int neighbourLevel);

    /**
     * Retrieves an entity representation (identifier, values and relations) with its direct neighbours from store.
     *
     * @param entityIri The unique entity URI
     * @param authentication   The current authentication
     * @return Entity as Mono
     */
    default Mono<Entity> get(IRI entityIri, Authentication authentication) {
        return this.get(entityIri, authentication, 1);
    }

    /**
     * Lists all entities
     *
     * @param authentication
     * @param limit
     * @param offset
     * @return
     */
     Flux<Entity> list(Authentication authentication, int limit, int offset);

    /**
     * Deletes an entity with all its values from the store.
     *
     * @param entityIri The unique entity identifier as IRI
     * @param authentication   The current authentication
     * @return Transaction with affected statements
     */
    Mono<Transaction> remove(IRI entityIri, Authentication authentication);

    /**
     * Deletes an entity with all its values from the store.
     *
     * @param entityKey The unique entity identifier as String
     * @param authentication   The current authentication
     * @return Transaction with affected statements
     */
    Mono<Transaction> remove(String entityKey, Authentication authentication);

    /**
     * Creates entities from the incoming set of triples
     *
     * @param triples        A Set of triples
     * @param parameters     Additional parameters coming through the request.
     * @param authentication The current authentication
     * @return Transaction with affected statements
     */
    Mono<Transaction> create(TripleBag triples, Map<String, String> parameters, Authentication authentication);

    Mono<Transaction> linkEntityTo(String entityIdentifier, IRI predicate, TripleBag linkedEntities, Authentication authentication);

    /**
     * Retrieves a complete entity representation (identifier, values and relations) from store.
     *
     * @param entityKey The unique entity key
     * @param authentication   The current authentication

     *
     * @return Entity as Mono
     */
    Mono<Entity> findByKey(String entityKey, Authentication authentication);

    Mono<Entity> findByProperty(String identifier, IRI predicate, Authentication authentication);

    Mono<Entity> find(String identifier, @Nullable String property, Authentication authentication);

    Mono<Boolean> contains(IRI entityIri, Authentication authentication);

    Mono<IRI> resolveAndVerify(String key, Authentication authentication);

    EntityStore getStore();
}
