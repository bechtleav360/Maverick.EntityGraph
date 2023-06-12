package org.av360.maverick.graph.store.behaviours;

import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import reactor.core.publisher.Mono;

import java.util.Set;


public interface ModelUpdates extends TripleStore {

    /**
     * Deletes the triples directly in the model (without transaction context)
     *
     * @param model
     * @return
     */
    Mono<Void> delete(Model statements, Authentication authentication, GrantedAuthority requiredAuthority);

    /**
     * Deletes the triples directly in the model (without transaction context)
     *
     * @param model
     * @return
     */
    default Mono<Void> delete(Set<Statement> statements, Authentication authentication, GrantedAuthority requiredAuthority) {
        return this.delete(new LinkedHashModel(statements), authentication, requiredAuthority);
    }

    /**
     * Stores the triples directly (without transaction context)
     */
    Mono<Void> insert(Model model, Authentication authentication, GrantedAuthority requiredAuthority);

    default Mono<Void> insert(Set<Statement> statements, Authentication authentication, GrantedAuthority requiredAuthority) {
        return this.insert(new LinkedHashModel(statements), authentication, requiredAuthority);
    }


    /**
     * Adds the triples in the model to the transaction. Don't forget to commit the transaction.
     *
     * @param model       the statements to store
     * @param transaction
     * @return Returns the transaction statements
     */
    Mono<RdfTransaction> insert(Set<Statement> model, RdfTransaction transaction);

}
