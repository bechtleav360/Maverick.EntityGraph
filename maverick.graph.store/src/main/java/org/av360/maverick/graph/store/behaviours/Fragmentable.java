package org.av360.maverick.graph.store.behaviours;


import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.store.rdf.fragments.RdfFragment;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Behaviour with all methods required to access and manipulate fragments. A fragment is the summary of all statements
 * which share the same subject.
 */
public interface Fragmentable extends RepositoryBehaviour {




    Mono<RdfFragment> getFragment(Resource subject, int includeNeighborsLevel, boolean includeDetails, Environment environment);

    Flux<RdfFragment> listFragments(IRI type, int limit, int offset, Environment environment);

    default Flux<RdfFragment> listFragments(Environment environment) {
        return this.listFragments(null, Integer.MAX_VALUE, 0, environment);
    }

    default Flux<RdfFragment> listFragments(IRI type, Environment environment) {
        return this.listFragments(type, Integer.MAX_VALUE, 0, environment);
    }


    default Mono<RdfFragment> getFragment(Resource subject, Environment environment) {
        return this.getFragment(subject, 0, false, environment);
    }

    Mono<Transaction> insertFragment(RdfFragment fragment, Environment environment);



    /**
     * Checks whether an fragment with the given identity exists, ie. we have an crdf:type statement.
     *
     * @param subj the id of the entity
     * @return true if exists
     */
    Mono<Boolean> exists(Resource subj, Environment environment);

    Mono<Long> countFragments(Environment environment);
}
