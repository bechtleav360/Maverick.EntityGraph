package org.av360.maverick.graph.store.services;

import org.apache.commons.lang3.Validate;
import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class StorageConfigurationService {

    private AppProperties appProperties;
    private StorageConfiguration defaultConfigurations;
    @Autowired
    public void setDefaultConfigurations(StorageConfiguration defaultConfigurations) {
        this.defaultConfigurations = defaultConfigurations;
    }

    @Autowired
    public void setMenu(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public void getConfiguration(RepositoryType repositoryType) {
    }




    public Mono<Environment> loadConfigurationFor(Environment environment) {


        StorageConfiguration.DefaultsConfiguration cfg = this.defaultConfigurations.getDefaultsConfigurations().stream().filter(it -> it.getLabel().equalsIgnoreCase(environment.getRepositoryType().toString())).findFirst().orElseThrow();
        environment.setFlag(Environment.RepositoryFlag.PERSISTENT, cfg.isPersistent());
        environment.setFlag(Environment.RepositoryFlag.REMOTE, cfg.isRemote());
        environment.setFlag(Environment.RepositoryFlag.PUBLIC, cfg.isPublished());


        if(cfg.isPersistent() && ! cfg.isRemote()) {
            Validate.notNull(cfg.getConfig().get("path"));
            environment.setConfiguration(Environment.RepositoryConfig.PATH, cfg.getConfig().get("path"));
        }

        return Mono.just(environment);
    }

}
