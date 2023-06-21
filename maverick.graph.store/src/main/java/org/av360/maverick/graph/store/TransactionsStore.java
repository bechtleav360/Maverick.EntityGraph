package org.av360.maverick.graph.store;

import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.store.behaviours.Maintainable;
import org.av360.maverick.graph.store.behaviours.ModelAware;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

public interface TransactionsStore extends Maintainable, ModelAware {


    @Deprecated
    default Mono<RdfTransaction> store(RdfTransaction transaction, Environment environment) {
        return this.store(List.of(transaction), environment).singleOrEmpty();
    }


    @Deprecated
    Flux<RdfTransaction> store(Collection<RdfTransaction> transaction, Environment environment);



}
