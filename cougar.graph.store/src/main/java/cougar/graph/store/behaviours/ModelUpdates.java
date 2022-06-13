package cougar.graph.store.behaviours;

import cougar.graph.model.enums.Activity;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.springframework.security.core.Authentication;
import org.springframework.util.Assert;
import cougar.graph.store.rdf.models.Transaction;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Collection;


public interface ModelUpdates extends RepositoryBehaviour {

    /**
     * Deletes the triples directly in the model (without transaction context)
     * @param model
     * @return
     */
    default Mono<Void> delete(Model model, Authentication authentication) {
        return Mono.create(sink -> {
            try (RepositoryConnection connection = getConnection(authentication)) {
                try {
                    Resource[] contexts = model.contexts().toArray(new Resource[model.contexts().size()]);
                    connection.add(model, contexts);
                    connection.commit();
                    sink.success();
                } catch (Exception e) {
                    connection.rollback();
                    sink.error(e);
                }
            } catch (RepositoryException e) {
                sink.error(e);
            } catch (IOException e) {
                sink.error(e);
            }
        });
    }

    default Mono<Transaction> delete(Collection<Statement> statements, Transaction transaction) {
        Assert.notNull(transaction, "Transaction cannot be null");


        return transaction
                .remove(statements, Activity.REMOVED)
                .asMono();
    }


    /**
     * Adds the triples in the model to the transaction. Don't forget to commit the transaction.
     *
     * @param model          the statements to store
     * @param transaction
     * @return Returns the transaction statements
     */
    default Mono<Transaction> insert(Model model, Transaction transaction) {
        Assert.notNull(transaction, "Transaction cannot be null");

        transaction = transaction
                .insert(model, Activity.INSERTED)
                .affected(model);


        return transaction.asMono();
    }

    /**
     *  Stores the triples directly (without transaction context)
     */
    Mono<Void> insert(Model model, Authentication authentication);
}
