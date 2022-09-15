package cougar.graph.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * We don't want any schedulers in our unit tests. The tests have to invoke the schedulers manually.
 */
@Configuration
@EnableScheduling
@Profile({"! test"})
public class SchedulerConfiguration {
}
