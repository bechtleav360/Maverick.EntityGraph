package io.av360.maverick.graph.feature;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConditionalOnProperty(
        name = "application.features.modules.webhooks"
)
@ComponentScan(basePackages = "io.av360.maverick.graph.feature.webhooks")
@Slf4j(topic = "graph.feature.webhooks")
public class WebhooksConfig {

    @PostConstruct
    public void logActivation() {
        log.info("Activated Feature: Multi-tenancy through Subscriptions");
    }
}
