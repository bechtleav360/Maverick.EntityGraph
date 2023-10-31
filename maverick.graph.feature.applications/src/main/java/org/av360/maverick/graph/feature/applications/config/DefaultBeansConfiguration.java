package org.av360.maverick.graph.feature.applications.config;

import org.av360.maverick.graph.feature.applications.services.ApplicationsService;
import org.av360.maverick.graph.feature.applications.services.delegates.*;
import org.av360.maverick.graph.services.*;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

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
        else if(bean instanceof JobSchedulingService delegate) {
            return new DelegatingJobSchedulingService(delegate);
        }
        else if(bean instanceof ConfigurationService delegate) {
            return new DelegatingConfigurationService(delegate, this.applicationsService);
        }
        else return bean;
    }

    @Bean
    public TaskScheduler taskScheduler() {
        return new ThreadPoolTaskScheduler();
    }


}


