package org.av360.maverick.graph;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConditionalOnProperty(
        name = "application.features.modules.admin.enabled"
)
@ComponentScan(basePackages = "org.av360.maverick.graph.feature.admin")
@Slf4j(topic = "graph.feat.admin")
public class AdminConfig {

    @PostConstruct
    public void logActivation() {
        log.info("Activated Feature: Administrative Operations");
    }
}
