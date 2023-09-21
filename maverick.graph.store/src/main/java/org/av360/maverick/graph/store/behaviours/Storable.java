package org.av360.maverick.graph.store.behaviours;


import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.store.repository.StoreBuilder;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.slf4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

public interface Storable {

    Logger getLogger();

    String getDefaultStorageDirectory();

    default ValueFactory getValueFactory() {
        return SimpleValueFactory.getInstance();
    }


    Flux<Transaction> commit(Collection<Transaction> transactions, Environment environment, boolean merge);

    default Flux<Transaction> commit(Collection<Transaction> transactions, Environment environment) {
        return this.commit(transactions, environment, false);
    }

    default Mono<Transaction> commit(Transaction transaction, Environment environment) {
        return this.commit(List.of(transaction), environment).singleOrEmpty();
    }


    RepositoryType getRepositoryType();

    StoreBuilder getBuilder(Environment environment);

}
