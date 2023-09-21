package org.av360.maverick.graph.store.behaviours;


import org.av360.maverick.graph.model.context.Environment;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface Selectable extends Storable {


    /**
     * Checks whether an entity with the given identity exists, ie. we have an crdf:type statement.
     *
     * @param subj the id of the entity
     * @return true if exists
     */
    Mono<Boolean> exists(Resource subj, Environment environment);

    Flux<IRI> types(Resource subj, Environment environment);

}
