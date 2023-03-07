package io.av360.maverick.graph.services;

import io.av360.maverick.graph.store.rdf.models.Transaction;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;

public interface ValueServices {
    /**
     * Sets the value. Replaces an existing value with the same predicate, except a different @-tag has been set
     *
     * @param entityKey         The unique local identifier of the entity
     * @param property          Prefixed key of the predicate
     * @param value             The new value
     * @param languageTag       Optional language tag
     * @param authentication    The current authentication
     * @return The transaction information.
     */
    Mono<Transaction> insertLiteral(String entityKey, String property, String value, @Nullable String languageTag, Authentication authentication);

    /**
     * Sets the value. Replaces an existing value with the same predicate, except a different @-tag has been set
     *
     * @param entityKey The unique local identifier of the entity
     * @param property     Prefixed key of the predicate
     * @param targetKey       The target key
     * @param authentication   The current authentication
     * @return The transaction information.
     */
    Mono<Transaction> insertLink(String entityKey, String property, String targetKey, Authentication authentication);

    /**
     * @param entityIdentifier The unique local identifier of the entity
     * @param predicate        Qualified predicate from existing schema
     * @param value            The new value
     * @param authentication   The current authentication
     * @return The transaction information.
     */
    Mono<Transaction> insertValue(IRI entityIdentifier, IRI predicate, Value value, Authentication authentication);

    /**
     * @param entityKey         The unique local identifier of the entity
     * @param prefixedProperty  Prefixed name of the predicate
     * @param lang              Optional language tag
     * @param authentication    The current authentication
     * @return The transaction information.
     */
    Mono<Transaction> removeLiteral(String entityKey, String prefixedProperty, String lang, Authentication authentication);

    /**
     * @param entityIdentifier The unique and qualified local identifier of the entity
     * @param predicate        Qualified predicate from existing schema
     * @param lang             Optional language tag
     * @param authentication   The current authentication
     * @return The transaction information.
     */
    Mono<Transaction> removeValue(IRI entityIdentifier, IRI predicate, @Nullable String lang, Authentication authentication);


    /**
     * @param entityKey         The unique local identifier of the entity
     * @param prefixedProperty  Prefixed name of the predicate
     * @param targetKey         The target key
     * @param authentication    The current authentication
     * @return The transaction information.
     */
    Mono<Transaction> removeLink(String entityKey, String prefixedProperty, String targetKey, Authentication authentication);


    /**
     * @param entityIdentifier The unique local identifier of the entity
     * @param predicate        Qualified predicate from existing schema
     * @param oldValue         The value to be removed
     * @param newValue         The new value
     * @param authentication   The current authentication
     * @return The transaction information.
     */
    Mono<Transaction> replace(IRI entityIdentifier, IRI predicate, Value oldValue, Value newValue, Authentication authentication);

}
