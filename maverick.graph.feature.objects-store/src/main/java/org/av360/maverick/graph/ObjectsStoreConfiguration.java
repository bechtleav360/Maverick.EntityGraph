package org.av360.maverick.graph;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
        name = "application.features.modules.objects.enabled"
)
@ComponentScan(
        basePackages = "org.av360.maverick.graph.feature.objects"
)
@Slf4j(topic = "graph.feat.obj")
public class ObjectsStoreConfiguration {

    @PostConstruct
    public void logActivation() {
        log.info("Activated Feature: Objects store");
    }
}
