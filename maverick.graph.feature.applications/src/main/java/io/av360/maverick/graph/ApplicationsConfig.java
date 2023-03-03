package io.av360.maverick.graph;

//
// import javax.annotation.PostConstruct;

import io.av360.maverick.graph.api.controller.entities.Entities;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;


@Configuration
@ConditionalOnProperty(
        name = "application.features.modules.applications"
)
@ComponentScan(
        basePackages = "io.av360.maverick.graph.feature.applications"
)
@Slf4j(topic = "egr.feat.apps")
public class ApplicationsConfig {

    @PostConstruct
    public void logActivation() {
        log.info("Activated Feature: Multi-tenancy through Applications and Subscriptions");
    }
}
