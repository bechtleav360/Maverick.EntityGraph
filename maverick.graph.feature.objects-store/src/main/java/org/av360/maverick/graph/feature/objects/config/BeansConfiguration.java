package org.av360.maverick.graph.feature.objects.config;

import org.av360.maverick.graph.feature.objects.services.FileServices;
import org.av360.maverick.graph.services.EntityServices;
import org.av360.maverick.graph.services.SchemaServices;
import org.av360.maverick.graph.services.ValueServices;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;

public class BeansConfiguration implements BeanPostProcessor {

    @Autowired
    ValueServices valueServices;

    @Autowired
    EntityServices entityServices;

    @Autowired
    SchemaServices schemaServices;

    @Autowired
    FileServices fileServices;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {


        return bean;
    }
}
