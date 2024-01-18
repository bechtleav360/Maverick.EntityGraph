package org.av360.maverick.graph.main;

import com.microsoft.applicationinsights.attach.ApplicationInsights;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@ComponentScan(basePackages = "org.av360.maverick.graph",
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.av360.maverick.graph.feature.*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.av360.maverick.graph.store.*")
        })
@ConfigurationPropertiesScan
public class Application {


    public static void main(String[] args) {
        ApplicationInsights.attach();
        SpringApplication.run(Application.class, args);
    }

}
