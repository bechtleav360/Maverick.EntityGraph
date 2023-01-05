package io.av360.maverick.graph.feature.applications.store.rdf4j;

import io.av360.maverick.graph.feature.applications.store.ApplicationsStore;
import io.av360.maverick.graph.store.RepositoryType;
import io.av360.maverick.graph.store.rdf4j.repository.util.AbstractRepository;
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
