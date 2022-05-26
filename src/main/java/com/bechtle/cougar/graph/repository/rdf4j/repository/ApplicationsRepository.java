package com.bechtle.cougar.graph.repository.rdf4j.repository;

import com.bechtle.cougar.graph.repository.rdf4j.config.RepositoryConfiguration;
import com.bechtle.cougar.graph.repository.ApplicationsStore;
import org.springframework.stereotype.Component;

/**
 *
 */
@Component
public class ApplicationsRepository extends AbstractRepository implements ApplicationsStore {


    public ApplicationsRepository() {
        super(RepositoryConfiguration.RepositoryType.APPLICATION);
    }


}
