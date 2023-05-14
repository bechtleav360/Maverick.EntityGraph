package org.av360.maverick.graph;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConditionalOnProperty(
        name = "application.features.modules.navigation.enabled"
)
@ComponentScan(
        basePackages = "org.av360.maverick.graph.feature.navigation"
)
@Slf4j(topic = "graph.feat.nav")
public class NavigationConfig {

    @PostConstruct
    public void logActivation() {
        log.info("Activated Feature: Navigation in Browser");
    }
}
