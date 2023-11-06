package org.av360.maverick.graph.services;

import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.store.rdf.fragments.RdfEntity;
import org.av360.maverick.graph.store.rdf.fragments.TripleModel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.Set;

public interface ValueServices {
    /**
     * Sets the value. Replaces an existing value with the same predicate, except a different @-tag has been set
     *
     * @param entityKey      The unique local identifier of the entity
     * @param property       Prefixed key of the predicate
     * @param value          The new value
     * @param languageTag    Optional language tag
     * @param authentication The current authentication
     * @return The transaction information.
     */
    Mono<Transaction> insertValue(String entityKey,
                                  String property,
                                  String value,
                                  @Nullable String languageTag,
                                  @Nullable Boolean replace,
                                  SessionContext ctx);

    /**
     * Sets the value. Replaces an existing value with the same predicate, except a different @-tag has been set
     *
     * @param entityKey      The unique local identifier of the entity
     * @param property       Prefixed key of the predicate
     * @param targetKey      The target key
     * @param authentication The current authentication
     * @return The transaction information.
     */
    Mono<Transaction> insertLink(String entityKey, String property, String targetKey, @Nullable Boolean replace, SessionContext ctx);

    /**
     * @param entityIdentifier The unique local identifier of the entity
     * @param predicate        Qualified predicate from existing schema
     * @param value            The new value
     * @param authentication   The current authentication
     * @return The transaction information.
     */
    Mono<Transaction> insertValue(IRI entityIdentifier, IRI predicate, Value value, @Nullable Boolean replace, SessionContext ctx);


    /**
     * Inserts an annotation to an existing property
     *
     * @param id
     * @param prefixedValueKey
     * @param prefixedDetailKey
     * @param value
     * @param hash
     * @param ctx
     * @return
     */
    Mono<Transaction> insertDetail(String id,
                                   String prefixedValueKey,
                                   String prefixedDetailKey,
                                   String value,
                                   @Nullable String hash,
                                   SessionContext ctx);

    /**
     * Inserts a set of statement as embedded entity
     *
     * @param entityIdentifier The unique local identifier of the entity
     * @param predicate        Qualified predicate from existing schema
     * @param value            The new value
     * @param authentication   The current authentication
     * @return The transaction information.
     */
    Mono<Transaction> insertEmbedded(IRI entityIdentifier, IRI predicate, Resource value, Set<Statement> embedded, SessionContext ctx);

    /**
     * @param authentication   The current authentication
     * @param entityKey        The unique local identifier of the entity
     * @param prefixedProperty Prefixed name of the predicate
     * @param lang             Optional language tag
     * @param identifier
     * @return The transaction information.
     */
    Mono<Transaction> removeValue(String entityKey, String prefixedProperty, @Nullable String lang, @Nullable String identifier, SessionContext ctx);

    /**
     * @param entityIdentifier The unique and qualified local identifier of the entity
     * @param predicate        Qualified predicate from existing schema
     * @param lang             Optional language tag
     * @param identifier
     * @return The transaction information.
     */
    Mono<Transaction> removeValue(IRI entityIdentifier, IRI predicate, @Nullable String lang, String identifier, SessionContext ctx);


    /**
     * @param entityKey        The unique local identifier of the entity
     * @param prefixedValueKey Prefixed name of the predicate
     * @param targetKey        The target key
     * @param authentication   The current authentication
     * @return The transaction information.
     */
    Mono<Transaction> removeLink(String entityKey, String prefixedValueKey, String targetKey, SessionContext ctx);


    /**
     * Removes a detail associated with a given entity key, value predicate, and detail predicate.
     *
     * @param entityKey               The key of the entity from which the detail is to be removed.
     * @param prefixedValuePredicate  The prefixed value predicate associated with the entity.
     * @param prefixedDetailPredicate The prefixed detail predicate associated with the value predicate that needs to be removed.
     * @param valueHash               A hash value to ensure data integrity for the given detail.
     * @param ctx                     The session context containing session-related information.
     * @return A Mono of the resulting {@link Transaction} after the removal operation.
     */
    Mono<Transaction> removeDetail(String entityKey, String prefixedValuePredicate, String prefixedDetailPredicate, String valueHash, SessionContext ctx);

    /**
     * @param entityIdentifier The unique local identifier of the entity
     * @param predicate        Qualified predicate from existing schema
     * @param oldValue         The value to be removed
     * @param newValue         The new value
     * @param authentication   The current authentication
     * @return The transaction information.
     */
    Mono<Transaction> replace(IRI entityIdentifier, IRI predicate, Value oldValue, Value newValue, SessionContext ctx);

    Mono<RdfEntity> listLinks(String id, String prefixedKey, SessionContext ctx);


    Mono<TripleModel> listValues(String id, @Nullable String prefixedKey, SessionContext ctx);


}
