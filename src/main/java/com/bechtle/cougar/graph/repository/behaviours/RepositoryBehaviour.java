package com.bechtle.cougar.graph.repository.behaviours;


import brave.internal.Nullable;
import com.bechtle.cougar.graph.domain.model.wrapper.Transaction;
import com.bechtle.cougar.graph.repository.rdf4j.config.RepositoryConfiguration;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public interface RepositoryBehaviour {


    default ValueFactory getValueFactory() {
        return SimpleValueFactory.getInstance();
    }

    default ValueFactory getValueFactory(@Nullable Authentication authentication) throws IOException {
        if (authentication == null || !authentication.isAuthenticated()) return this.getValueFactory();

        return getConfiguration().getRepository(this.getRepositoryType(), authentication).getValueFactory();
    }

    /**
     * Checks whether an entity with the given identity exists, ie. we have an rdf:type statement.
     *
     * @param subj the id of the entity
     * @return true if exists
     */
    default Mono<Boolean> exists(Resource subj, Authentication authentication) throws IOException {
        try (RepositoryConnection connection = getConnection(authentication)) {
            return Mono.just(connection.hasStatement(subj, RDF.TYPE, null, false));
        }
    }

    /**
     * Returns the type of the entity identified by the given id;
     *
     * @param identifier the id of the entity
     * @return its type (or empty)
     */
    default Mono<Value> type(Resource identifier, Authentication authentication) {
        try (RepositoryConnection connection = getConnection(authentication)) {
            RepositoryResult<Statement> statements = connection.getStatements(identifier, RDF.TYPE, null, false);

            Value result = null;
            for (Statement st : statements) {
                // FIXME: not sure if this is a domain exception (which mean it should not be handled here)
                if (result != null) {
                    return Mono.error(new IOException("Duplicate type definitions for resource with identifier " + identifier.stringValue()));
                } else result = st.getObject();
            }
            if (result == null) return Mono.empty();
            else return Mono.just(result);


        } catch (Exception e) {
            return Mono.error(e);
        }

    }


    Flux<Transaction> commit(Collection<Transaction> transactions, Authentication authentication);

    default Mono<Transaction> commit(Transaction transaction, Authentication authentication) {
        return this.commit(List.of(transaction), authentication).singleOrEmpty();
    }


    default RepositoryConnection getConnection(Authentication authentication) throws IOException {
        return getConfiguration().getRepository(getRepositoryType(), authentication).getConnection();
    }

    default RepositoryConnection getConnection(Authentication authentication, RepositoryConfiguration.RepositoryType repositoryType) throws IOException {
        return getConfiguration().getRepository(repositoryType, authentication).getConnection();
    }

    RepositoryConfiguration.RepositoryType getRepositoryType();

    RepositoryConfiguration getConfiguration();

}
