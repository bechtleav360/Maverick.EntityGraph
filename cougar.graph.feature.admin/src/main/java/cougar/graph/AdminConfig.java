package cougar.graph;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
@ConditionalOnProperty(
        name = "application.features.modules.admin",
        matchIfMissing = false)
@ComponentScan(basePackages = "cougar.graph.feature.admin")
@Slf4j(topic = "cougar.graph")
public class AdminConfig {

    @PostConstruct
    public void logActivation() {
        log.info("Active Feature: Administrative Operations");
    }
}
