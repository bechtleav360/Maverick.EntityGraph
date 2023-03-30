package org.av360.maverick.graph.feature.applications.config;

import org.av360.maverick.graph.feature.applications.decorators.DelegatingIdentifierFactory;
import org.av360.maverick.graph.model.identifier.IdentifierFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

public class DefaultBeansConfiguration implements BeanPostProcessor {
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if(bean instanceof IdentifierFactory identifierFactory) {
            return new DelegatingIdentifierFactory(identifierFactory);
        }

        else if(bean instanceof IdentifierFactory identifierFactory) {
            return new DelegatingIdentifierFactory(identifierFactory);
        }


        return bean;
    }
}


