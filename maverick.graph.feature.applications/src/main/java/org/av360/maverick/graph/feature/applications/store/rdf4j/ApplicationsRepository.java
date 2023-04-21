package org.av360.maverick.graph.feature.applications.store.rdf4j;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.applications.store.ApplicationsStore;
import org.av360.maverick.graph.store.RepositoryType;
import org.av360.maverick.graph.store.rdf4j.repository.util.AbstractRepository;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

/**
 *
 */
@Component
@Slf4j(topic = "graph.repo.applications")
public class ApplicationsRepository extends AbstractRepository implements ApplicationsStore {


    public ApplicationsRepository() {
        super(RepositoryType.APPLICATION);
    }


    @Override
    public Logger getLogger() {
        return log;
    }
}
