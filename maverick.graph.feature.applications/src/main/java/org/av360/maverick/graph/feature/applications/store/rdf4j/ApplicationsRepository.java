package org.av360.maverick.graph.feature.applications.store.rdf4j;

import org.av360.maverick.graph.feature.applications.store.ApplicationsStore;
import org.av360.maverick.graph.store.RepositoryType;
import org.av360.maverick.graph.store.rdf4j.repository.util.AbstractRepository;
import org.springframework.stereotype.Component;

/**
 *
 */
@Component
public class ApplicationsRepository extends AbstractRepository implements ApplicationsStore {


    public ApplicationsRepository() {
        super(RepositoryType.APPLICATION);
    }


}
