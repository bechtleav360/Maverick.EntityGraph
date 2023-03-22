package io.av360.maverick.graph.store;

import org.eclipse.rdf4j.repository.Repository;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

public interface RepositoryBuilder {


    Mono<Repository> buildRepository(RepositoryType repositoryType, Authentication authentication);


}
