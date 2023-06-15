package org.av360.maverick.graph.services;

import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.store.rdf.fragments.RdfEntity;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
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
     * @param entityKey         The unique local identifier of the entity
     * @param property          Prefixed key of the predicate
     * @param value             The new value
     * @param languageTag       Optional language tag
     * @param authentication    The current authentication
     * @return The transaction information.
     */
    Mono<RdfTransaction> insertValue(String entityKey, String property, String value, @Nullable String languageTag, SessionContext ctx);

    /**
     * Sets the value. Replaces an existing value with the same predicate, except a different @-tag has been set
     *
     * @param entityKey The unique local identifier of the entity
     * @param property     Prefixed key of the predicate
     * @param targetKey       The target key
     * @param authentication   The current authentication
     * @return The transaction information.
     */
    Mono<RdfTransaction> insertLink(String entityKey, String property, String targetKey, SessionContext ctx);

    /**
     * @param entityIdentifier The unique local identifier of the entity
     * @param predicate        Qualified predicate from existing schema
     * @param value            The new value
     * @param authentication   The current authentication
     * @return The transaction information.
     */
    Mono<RdfTransaction> insertValue(IRI entityIdentifier, IRI predicate, Value value, SessionContext ctx);


    /**
     * Inserts a set of statement as embedded entity
     *
     * @param entityIdentifier The unique local identifier of the entity
     * @param predicate        Qualified predicate from existing schema
     * @param value            The new value
     * @param authentication   The current authentication
     * @return The transaction information.
     */
    Mono<RdfTransaction> insertEmbedded(IRI entityIdentifier, IRI predicate, Resource value, Set<Statement> embedded, SessionContext ctx);

    /**
     * @param entityKey         The unique local identifier of the entity
     * @param prefixedProperty  Prefixed name of the predicate
     * @param lang              Optional language tag
     * @param authentication    The current authentication
     * @return The transaction information.
     */
    Mono<RdfTransaction> removeLiteral(String entityKey, String prefixedProperty, String lang, SessionContext ctx);

    /**
     * @param entityIdentifier The unique and qualified local identifier of the entity
     * @param predicate        Qualified predicate from existing schema
     * @param lang             Optional language tag
     * @param authentication   The current authentication
     * @return The transaction information.
     */
    Mono<RdfTransaction> removeValue(IRI entityIdentifier, IRI predicate, @Nullable String lang, SessionContext ctx);


    /**
     * @param entityKey         The unique local identifier of the entity
     * @param prefixedProperty  Prefixed name of the predicate
     * @param targetKey         The target key
     * @param authentication    The current authentication
     * @return The transaction information.
     */
    Mono<RdfTransaction> removeLink(String entityKey, String prefixedProperty, String targetKey, SessionContext ctx);


    /**
     * @param entityIdentifier The unique local identifier of the entity
     * @param predicate        Qualified predicate from existing schema
     * @param oldValue         The value to be removed
     * @param newValue         The new value
     * @param authentication   The current authentication
     * @return The transaction information.
     */
    Mono<RdfTransaction> replace(IRI entityIdentifier, IRI predicate, Value oldValue, Value newValue, SessionContext ctx);

    Mono<RdfEntity> listLinks(String id, String prefixedKey, SessionContext ctx);
}
