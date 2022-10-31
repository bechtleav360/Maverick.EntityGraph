package io.av360.maverick.graph.store.behaviours;

import org.eclipse.rdf4j.model.Model;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import reactor.core.publisher.Mono;


public interface ModelUpdates extends RepositoryBehaviour {

    /**
     * Deletes the triples directly in the model (without transaction context)
     *
     * @param model
     * @return
     */
    Mono<Void> delete(Model model, Authentication authentication, GrantedAuthority requiredAuthority);

    /**
     * Stores the triples directly (without transaction context)
     */
    Mono<Void> insert(Model model, Authentication authentication, GrantedAuthority requiredAuthority);




}
