package org.av360.maverick.graph.store.behaviours;


import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.store.rdf.fragments.RdfEntity;
import org.eclipse.rdf4j.model.Resource;
import reactor.core.publisher.Mono;

/**
 * Fragments are all statements which share the same subject
 */
public interface FragmentsAware {

    Mono<RdfEntity> getFragment(Resource subject, int includeNeighborsLevel, boolean includeDetails, Environment environment);


    default Mono<RdfEntity> getFragment(Resource subject, Environment environment) {
        return this.getFragment(subject, 0, false, environment);
    }


}
