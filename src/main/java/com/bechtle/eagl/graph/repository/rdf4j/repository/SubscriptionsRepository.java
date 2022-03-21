package com.bechtle.eagl.graph.repository.rdf4j.repository;

import com.bechtle.eagl.graph.repository.rdf4j.config.RepositoryConfiguration;
import com.bechtle.eagl.graph.features.multitenancy.repository.ApplicationsStore;
import org.springframework.stereotype.Component;

/**
 *
 */
@Component
public class SubscriptionsRepository extends AbstractRepository implements ApplicationsStore {


    public SubscriptionsRepository() {
        super(RepositoryConfiguration.RepositoryType.SUBSCRIPTIONS);
    }


}
