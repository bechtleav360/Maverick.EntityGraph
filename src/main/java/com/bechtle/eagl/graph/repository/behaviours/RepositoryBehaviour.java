package com.bechtle.eagl.graph.repository.behaviours;


import com.bechtle.eagl.graph.domain.model.wrapper.Transaction;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public interface RepositoryBehaviour {

    Repository getRepository();


    default ValueFactory getValueFactory() {
        return getRepository().getValueFactory();
    }

    /**
     * Checks whether an entity with the given identity exists, ie. we have an rdf:type statement.
     * @param subj the id of the entity
     * @return true if exists
     */
    default Mono<Boolean> exists(Resource subj) {
        return Mono.create(m -> {
            try (RepositoryConnection connection = getRepository().getConnection()) {
                boolean b = connection.hasStatement(subj, RDF.TYPE, null, false);
                m.success(b);
            } catch (Exception e) {
                m.error(e);
            }
        });
    }

    /**
     * Returns the type of the entity identified by the given id;
     * @param identifier the id of the entity
     * @return its type (or empty)
     */
    default Mono<Value> type(Resource identifier) {
        return Mono.create(m -> {
            try (RepositoryConnection connection = getRepository().getConnection()) {
                RepositoryResult<Statement> statements = connection.getStatements(identifier, RDF.TYPE, null, false);

                Value result = null;
                for (Statement st : statements) {
                    // FIXME: not sure if this is a domain exception (which mean it should not be handled here)
                    if (result != null)
                        m.error(new IOException("Duplicate type definitions for resource with identifier " + identifier.stringValue()));
                    else result = st.getObject();
                }
                if (result == null) m.success();
                else m.success(result);


            } catch (Exception e) {
                m.error(e);
            }
        });
    }

    Flux<Transaction> commit(Collection<Transaction> transactions);

    default Mono<Transaction> commit(Transaction transaction) {
        return this.commit(List.of(transaction)).singleOrEmpty();
    }

}
