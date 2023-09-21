package org.av360.maverick.graph.feature.applications.store.rdf4j;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.applications.store.ApplicationsStore;
import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.store.rdf4j.repository.util.SailStore;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 *
 */
@Component
@Slf4j(topic = "graph.repo.applications")
public class ApplicationsRepository extends SailStore implements ApplicationsStore {

    @Value("${application.storage.system.path:#{null}}")
    private String path;
    public ApplicationsRepository() {
        super(RepositoryType.SYSTEM);
    }



    @Override
    public Logger getLogger() {
        return log;
    }

    @Override
    protected void addDefaultStorageConfiguration(Environment environment) {

    }

    @Override
    public String getDefaultStorageDirectory() {
        if(StringUtils.hasLength(this.path)) return this.path+"/applications";
        else return "";
    }


}
