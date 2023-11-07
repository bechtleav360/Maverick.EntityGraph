package org.av360.maverick.graph.store.behaviours;

import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.entities.Transaction;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import reactor.core.publisher.Mono;

import java.util.Set;


public interface ModelAware extends TripleStore {

    /**
     * Deletes the triples directly in the model (without transaction context)
     *
     * @param model
     * @return
     */
    Mono<Void> deleteModel(Model statements, Environment environment);


    /**
     * Stores the triples directly (without transaction context)
     */
    Mono<Void> insertModel(Model model, Environment environment);

    /**
     * Adds the triples in the model to the transaction. Don't forget to commit the transaction.
     *
     * @param model       the statements to store
     * @param transaction
     * @return Returns the transaction statements
     */
    Mono<Transaction> insertModel(Model model, Transaction transaction);

    Mono<Model> getModel(Environment environment);

    default Mono<Void> insertModel(Set<Statement> statements, Environment environment) {
        return this.insertModel(new LinkedHashModel(statements), environment);
    }

    default Mono<Void> deleteModel(Set<Statement> statements, Environment environment) {
        return this.deleteModel(new LinkedHashModel(statements), environment);
    }



}
