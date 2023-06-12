package org.av360.maverick.graph.feature.applications.config;

import org.av360.maverick.graph.feature.applications.decorators.DelegatingContentResolver;
import org.av360.maverick.graph.feature.applications.decorators.DelegatingIdentifierServices;
import org.av360.maverick.graph.feature.applications.decorators.DelegatingJobsService;
import org.av360.maverick.graph.feature.applications.decorators.DelegatingNavigationServices;
import org.av360.maverick.graph.feature.applications.services.ApplicationsService;
import org.av360.maverick.graph.services.ContentLocationResolverService;
import org.av360.maverick.graph.services.IdentifierServices;
import org.av360.maverick.graph.services.JobSchedulingService;
import org.av360.maverick.graph.services.NavigationServices;
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
            return new DelegatingJobsService(delegate);
        }
        else return bean;
    }

    @Bean
    public TaskScheduler taskScheduler() {
        return new ThreadPoolTaskScheduler();
    }


}


