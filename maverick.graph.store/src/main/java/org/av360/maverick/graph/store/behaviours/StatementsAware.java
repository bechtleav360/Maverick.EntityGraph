package org.av360.maverick.graph.store.behaviours;

import org.av360.maverick.graph.model.context.Environment;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import reactor.core.publisher.Mono;

import java.util.Set;

public interface StatementsAware extends RepositoryBehaviour {

    default ValueFactory getValueFactory() {
        return SimpleValueFactory.getInstance();
    }



    Mono<Set<Statement>> listStatements(Resource subject, IRI predicate, Value object, Environment environment);



    Mono<Boolean> hasStatement(Resource subject, IRI predicate, Value object, Environment environment);







}
