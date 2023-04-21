package org.av360.maverick.graph.services;

import jakarta.annotation.Nullable;
import org.av360.maverick.graph.store.EntityStore;
import org.av360.maverick.graph.store.rdf.fragments.RdfEntity;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.av360.maverick.graph.store.rdf.fragments.TripleBag;
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
    Mono<RdfEntity> get(IRI entityIri, Authentication authentication, int neighbourLevel);

    /**
     * Retrieves an entity representation (identifier, values and relations) with its direct neighbours from store.
     *
     * @param entityIri The unique entity URI
     * @param authentication   The current authentication
     * @return Entity as Mono
     */
    default Mono<RdfEntity> get(IRI entityIri, Authentication authentication) {
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
     Flux<RdfEntity> list(Authentication authentication, int limit, int offset);

    /**
     * Deletes an entity with all its values from the store.
     *
     * @param entityIri The unique entity identifier as IRI
     * @param authentication   The current authentication
     * @return Transaction with affected statements
     */
    Mono<RdfTransaction> remove(IRI entityIri, Authentication authentication);

    /**
     * Deletes an entity with all its values from the store.
     *
     * @param entityKey The unique entity identifier as String
     * @param authentication   The current authentication
     * @return Transaction with affected statements
     */
    Mono<RdfTransaction> remove(String entityKey, Authentication authentication);

    /**
     * Creates entities from the incoming set of triples
     *
     * @param triples        A Set of triples
     * @param parameters     Additional parameters coming through the request.
     * @param authentication The current authentication
     * @return Transaction with affected statements
     */
    Mono<RdfTransaction> create(TripleBag triples, Map<String, String> parameters, Authentication authentication);

    Mono<RdfTransaction> linkEntityTo(String entityKey, IRI predicate, TripleBag linkedEntities, Authentication authentication);

    /**
     * Retrieves a complete entity representation (identifier, values and relations) from store.
     *
     * @param entityKey The unique entity key
     * @param authentication   The current authentication

     *
     * @return Entity as Mono
     */
    Mono<RdfEntity> findByKey(String entityKey, Authentication authentication);

    Mono<RdfEntity> findByProperty(String identifier, IRI predicate, Authentication authentication);

    Mono<RdfEntity> find(String entityKey, @Nullable String property, Authentication authentication);

    Mono<Boolean> contains(IRI entityIri, Authentication authentication);

    Mono<IRI> resolveAndVerify(String entityKey, Authentication authentication);

    EntityStore getStore();

}
