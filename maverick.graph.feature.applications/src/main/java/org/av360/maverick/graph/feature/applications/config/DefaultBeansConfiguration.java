package org.av360.maverick.graph.feature.applications.config;

import org.av360.maverick.graph.feature.applications.decorators.DelegatingContentResolver;
import org.av360.maverick.graph.feature.applications.decorators.DelegatingIdentifierServices;
import org.av360.maverick.graph.feature.applications.decorators.DelegatingNavigationServices;
import org.av360.maverick.graph.feature.applications.domain.ApplicationsService;
import org.av360.maverick.graph.services.ContentLocationResolverService;
import org.av360.maverick.graph.services.IdentifierServices;
import org.av360.maverick.graph.services.NavigationServices;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DefaultBeansConfiguration implements BeanPostProcessor {


    @Autowired
    ApplicationsService applicationsService;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
       return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if(bean instanceof NavigationServices delegate) {
            return new DelegatingNavigationServices(delegate, this.applicationsService);
        }
        else if(bean instanceof IdentifierServices delegate) {
            return new DelegatingIdentifierServices(delegate);
        }
        else if(bean instanceof ContentLocationResolverService delegate) {
            return new DelegatingContentResolver(delegate, this.applicationsService);
        }
        else return bean;
    }


}


