package io.av360.maverick.graph;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConditionalOnProperty(
        name = "application.features.modules.admin"
)
@ComponentScan(basePackages = "io.av360.maverick.graph.feature.admin")
@Slf4j(topic = "graph.feature.admin")
public class AdminConfig {

    @PostConstruct
    public void logActivation() {
        log.info("Activated Feature: Administrative Operations");
    }
}
