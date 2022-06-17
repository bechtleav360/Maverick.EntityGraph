package cougar.graph.store.behaviours;

import cougar.graph.model.enums.Activity;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;
import cougar.graph.store.rdf.models.Transaction;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Collection;


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
