package io.av360.maverick.graph;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConditionalOnProperty(
        name = "application.features.modules.applications"
)
@ComponentScan(basePackages = "cougar.graph.feature.applications")
@Slf4j(topic = "graph.feature.apps")
public class ApplicationsConfig {

    @PostConstruct
    public void logActivation() {
        log.info("Activated Feature: Multi-tenancy through Subscriptions");
    }
}
