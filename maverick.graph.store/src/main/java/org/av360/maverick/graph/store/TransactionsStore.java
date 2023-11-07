package org.av360.maverick.graph.store;

import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.store.behaviours.Maintainable;
import org.av360.maverick.graph.store.behaviours.ModelAware;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

public interface TransactionsStore extends Maintainable, ModelAware {


    @Deprecated
    default Mono<Transaction> store(Transaction transaction, Environment environment) {
        return this.store(List.of(transaction), environment).singleOrEmpty();
    }


    Flux<Transaction> store(Collection<Transaction> transaction, Environment environment);



}
