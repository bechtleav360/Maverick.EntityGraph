package io.av360.maverick.graph.services;

import io.av360.maverick.graph.store.rdf.models.Transaction;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;

public interface ValueServices {
    /**
     * Sets the new value. Replaces an existing value with the same predicate, except a different @-tag has been set
     *
     * @param entityIdentifier The unique local identifier of the entity
     * @param predicatePrefix  Prefix of the predicate
     * @param predicateKey     Key of the predicate
     * @param value            The new value
     * @param languageTag
     * @param authentication   The current authentication
     * @return The transaction information.
     */
    Mono<Transaction> insertValue(String entityIdentifier, String predicatePrefix, String predicateKey, String value, @Nullable String languageTag, Authentication authentication);

    /**
     * @param entityIdentifier The unique local identifier of the entity
     * @param predicate        Qualified predicate from existing schema
     * @param value            The new value
     * @param authentication   The current authentication
     * @return The transaction information.
     */
    Mono<Transaction> insertValue(Resource entityIdentifier, IRI predicate, Value value, Authentication authentication);

    /**
     * @param entityIdentifier The unique local identifier of the entity
     * @param predicatePrefix  Prefix of the predicate
     * @param predicateKey     Key of the predicate
     * @param lang             Optional language tag
     * @param authentication   The current authentication
     * @return The transaction information.
     */
    Mono<Transaction> removeValue(String entityIdentifier, String predicatePrefix, String predicateKey, String lang, Authentication authentication);

    /**
     * @param entityIdentifier The unique local identifier of the entity
     * @param predicate        Qualified predicate from existing schema
     * @param lang             Optional language tag
     * @param authentication   The current authentication
     * @return The transaction information.
     */
    Mono<Transaction> removeValue(Resource entityIdentifier, IRI predicate, String lang, Authentication authentication);

    /**
     * @param entityIdentifier The unique local identifier of the entity
     * @param predicate        Qualified predicate from existing schema
     * @param oldValue         The value to be removed
     * @param newValue         The new value
     * @param authentication   The current authentication
     * @return The transaction information.
     */
    Mono<Transaction> replaceValue(Resource entityIdentifier, IRI predicate, Value oldValue, Value newValue, Authentication authentication);

}
