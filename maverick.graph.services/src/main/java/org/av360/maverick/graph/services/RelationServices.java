package org.av360.maverick.graph.services;

import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.av360.maverick.graph.store.rdf.fragments.TripleBag;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

public interface RelationServices {

    Mono<RdfTransaction> link(String entityIdentifier, String predicatePrefix, String predicateKey, TripleBag linkedEntities, Authentication authentication);

    Mono<RdfTransaction> link(Resource entityIdentifier, IRI predicate, TripleBag linkedEntities, Authentication authentication);


    Mono<RdfTransaction> unlink(String entityIdentifier, String predicatePrefix, String predicateKey, TripleBag linkedEntities, Authentication authentication);

    Mono<RdfTransaction> unlink(Resource entityIdentifier, IRI predicate, TripleBag linkedEntities, Authentication authentication);

    Mono<RdfTransaction> embed(Resource entityIdentifier, IRI predicate, TripleBag embeddedEntities, Authentication authentication);


}
