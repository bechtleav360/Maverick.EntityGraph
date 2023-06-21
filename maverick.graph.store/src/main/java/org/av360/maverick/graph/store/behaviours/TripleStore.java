package org.av360.maverick.graph.store.behaviours;


import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.store.RepositoryBuilder;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.slf4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Set;

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

    Flux<RdfTransaction> commit(Collection<RdfTransaction> transactions, Environment environment, boolean merge);

    default Flux<RdfTransaction> commit(Collection<RdfTransaction> transactions, Environment environment) {
        return this.commit(transactions, environment, false);
    }

    default Mono<RdfTransaction> commit(RdfTransaction transaction, Environment environment) {
        return this.commit(List.of(transaction), environment).singleOrEmpty();
    }

    Mono<Set<Statement>> listStatements(Resource subject, IRI predicate, Value object, Environment environment);

    Mono<RdfTransaction> removeStatements(Collection<Statement> statements, RdfTransaction transaction);

    Mono<Boolean> hasStatement(Resource subject, IRI predicate, Value object, Environment environment);

    RepositoryType getRepositoryType();

    RepositoryBuilder getBuilder();

}
