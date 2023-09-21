package org.av360.maverick.graph.store.services;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.apache.commons.lang3.Validate;

import java.util.List;
import java.util.Map;


@Data
public class StorageConfiguration {
    // https://mkyong.com/spring-boot/spring-boot-configurationproperties-example/
    // https://stackoverflow.com/questions/59073648/springboot-failed-to-bind-properties-under-app

    List<DefaultsConfiguration> defaults;

    public List<RemoteConfiguration> getRemotes() {
        return remotes;
    }

    List<RemoteConfiguration> remotes;

    public List<DefaultsConfiguration> getDefaultsConfigurations() {
        Validate.notEmpty(this.defaults, "No configuration for default repositories found");
        return defaults;
    }

    @PostConstruct
    private void validateDefaultStorage() {

    }





    @Data
    public static class DefaultsConfiguration {
        String label;
        boolean persistent;
        boolean remote;
        boolean published;
        Map<String, String> config;
    }

    @Data
    public static class RemoteConfiguration {
        String label;
        Map<String, String> endpoints;
    }

}
