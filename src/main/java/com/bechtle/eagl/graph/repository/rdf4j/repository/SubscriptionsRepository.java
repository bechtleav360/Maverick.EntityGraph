package com.bechtle.eagl.graph.repository.rdf4j.repository;

import com.bechtle.eagl.graph.repository.rdf4j.config.RepositoryConfiguration;
import com.bechtle.eagl.graph.subscriptions.repository.SubscriptionsStore;
import org.springframework.stereotype.Component;

/**
 *
 */
@Component
public class SubscriptionsRepository extends AbstractRepository implements SubscriptionsStore {


    public SubscriptionsRepository() {
        super(RepositoryConfiguration.RepositoryType.SUBSCRIPTIONS);
    }


}
