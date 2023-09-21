package org.av360.maverick.graph.store.config;

import org.av360.maverick.graph.store.services.AppProperties;
import org.av360.maverick.graph.store.services.StorageConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PropertyBeans {

    @Bean
    @ConfigurationProperties(value = "app", ignoreUnknownFields = true) // prefix app, find app.* values
    public AppProperties setAppProperties() {
        return new AppProperties();
    }


    @Bean
    @ConfigurationProperties(prefix = "application.storage") // prefix app, find app.* values
    public StorageConfiguration defaultStorageProperties() {
        return new StorageConfiguration();
    }
}
