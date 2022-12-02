package io.av360.maverick.graph.services;

import io.av360.maverick.graph.store.rdf.models.Incoming;
import io.av360.maverick.graph.store.rdf.models.Transaction;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

public interface RelationServices {

    Mono<Transaction> link(String entityIdentifier, String predicatePrefix, String predicateKey, Incoming linkedEntities, Authentication authentication);

    Mono<Transaction> link(Resource entityIdentifier, IRI predicate, Incoming linkedEntities, Authentication authentication);


    Mono<Transaction> unlink(String entityIdentifier, String predicatePrefix, String predicateKey, Incoming linkedEntities, Authentication authentication);

    Mono<Transaction> unlink(Resource entityIdentifier, IRI predicate, Incoming linkedEntities, Authentication authentication);


    Mono<Transaction> embed(Resource entityIdentifier, IRI predicate, Incoming embeddedEntities, Authentication authentication);





}
