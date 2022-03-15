package com.bechtle.eagl.graph.repository.rdf4j.repository;

import com.bechtle.eagl.graph.repository.SubscriptionsStore;
import org.eclipse.rdf4j.repository.Repository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class SubscriptionsRepository extends AbstractRepository implements SubscriptionsStore {


    public SubscriptionsRepository(@Qualifier("subscriptions-storage") Repository repository) {
        super(repository);
    }


}
