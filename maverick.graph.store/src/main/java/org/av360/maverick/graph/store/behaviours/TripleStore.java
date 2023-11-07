package org.av360.maverick.graph.store.behaviours;


import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.store.RepositoryBuilder;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.slf4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

public interface TripleStore {

    Logger getLogger();

    String getDirectory();

    default ValueFactory getValueFactory() {
        return SimpleValueFactory.getInstance();
    }


    /**
     * Checks whether an entity with the given identity exists, ie. we have an crdf:type statement.
     *
     * @param subj the id of the entity
     * @return true if exists
     */
    Mono<Boolean> exists(Resource subj, Environment environment);


    Flux<IRI> types(Resource subj, Environment environment);

    Flux<Transaction> commit(Collection<Transaction> transactions, Environment environment, boolean merge);

    default Flux<Transaction> commit(Collection<Transaction> transactions, Environment environment) {
        return this.commit(transactions, environment, false);
    }

    default Mono<Transaction> commit(Transaction transaction, Environment environment) {
        return this.commit(List.of(transaction), environment).singleOrEmpty();
    }





    Mono<Boolean> hasStatement(Resource subject, IRI predicate, Value object, Environment environment);

    RepositoryType getRepositoryType();

    RepositoryBuilder getBuilder();

}
