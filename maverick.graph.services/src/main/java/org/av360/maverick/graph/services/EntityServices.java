package org.av360.maverick.graph.services;

import jakarta.annotation.Nullable;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.model.rdf.Triples;
import org.av360.maverick.graph.store.IndividualsStore;
import org.av360.maverick.graph.store.rdf.fragments.Fragment;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.core.io.Resource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface EntityServices {


    /**
     * Retrieves a complete entity representation (identifier, values and relations) from store.
     *
     * @param authentication The current authentication
     * @param entityIri      The unique entity URI
     * @param details
     * @param depth          how many levels of neigbours to include (0 is entity only, 1 is direct neighbours)
     * @return Entity as Mono
     */

    Mono<Fragment> get(IRI entityIri, boolean details, int depth, SessionContext ctx);

    /**
     * Retrieves an entity representation (identifier, values and relations) with its direct neighbours from store.
     *
     * @param entityIri The unique entity URI
     * @param authentication   The current authentication
     * @return Entity as Mono
     */
    default Mono<Fragment> get(IRI entityIri, SessionContext ctx) {
        return this.get(entityIri, false, 1, ctx);
    }

    /**
     * Lists all entities using the default query
     *
     * @param authentication
     * @param limit
     * @param offset
     * @return
     */
     Flux<Fragment> list(int limit, int offset, SessionContext ctx);


    /**
     * Lists all entities using the given query
     *
     * @param authentication
     * @param limit
     * @param offset
     * @return
     */
    Flux<Fragment> list(int limit, int offset, SessionContext ctx, String query);

    /**
     * Deletes an entity with all its values from the store.
     *
     * @param entityIri      The unique entity identifier as IRI
     * @param authentication The current authentication
     * @return Transaction with affected statements
     */
    Mono<Transaction> remove(IRI entityIri, SessionContext ctx);

    /**
     * Deletes an entity with all its values from the store.
     *
     * @param entityKey      The unique entity identifier as String
     * @param authentication The current authentication
     * @return Transaction with affected statements
     */
    Mono<Transaction> remove(String entityKey, SessionContext ctx);

    /**
     * Creates entities from the incoming set of triples
     *
     * @param triples        A Set of triples
     * @param parameters     Additional parameters coming through the request.
     * @param authentication The current authentication
     * @return Transaction with affected statements
     */
    Mono<Transaction> create(Triples triples, Map<String, String> parameters, SessionContext ctx);


    Mono<Transaction> linkEntityTo(String entityKey, IRI predicate, Triples linkedEntities, SessionContext ctx);


    /**
     * Retrieves a complete entity representation (identifier, values and relations) from store.
     *
     * @param authentication The current authentication
     * @param entityKey      The unique entity key
     * @param details
     * @param depth
     * @return Entity as Mono
     */

    Mono<Fragment> findByKey(String entityKey, boolean details, int depth, SessionContext ctx);

    Mono<Fragment> findByProperty(String identifier, IRI predicate, SessionContext ctx);


    Mono<Fragment> find(String entityKey, @Nullable String property,  boolean details, int depth, SessionContext ctx);

    Mono<Boolean> contains(IRI entityIri, SessionContext ctx);

    Mono<IRI> resolveAndVerify(String entityKey, SessionContext ctx);

    IndividualsStore getStore(SessionContext ctx);

    Mono<Transaction> importFile(Resource resource, RDFFormat format, SessionContext ctx);

    /**
     * Returns the complete content
     * @param ctx
     * @return
     */
    Mono<Model> asModel(SessionContext ctx);
}
