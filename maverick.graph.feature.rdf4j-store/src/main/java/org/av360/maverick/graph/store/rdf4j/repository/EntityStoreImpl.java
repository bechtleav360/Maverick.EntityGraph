package org.av360.maverick.graph.store.rdf4j.repository;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.store.PersistedEntityGraph;
import org.av360.maverick.graph.store.rdf4j.repository.util.SailStore;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

@Slf4j(topic = "graph.repo.entities")
@Component
public class EntityStoreImpl extends SailStore implements PersistedEntityGraph {


    public EntityStoreImpl() {
        super(RepositoryType.ENTITIES);
    }


    @Override
    public Logger getLogger() {
        return log;
    }


    @Override
    protected void addDefaultStorageConfiguration(Environment environment) {
        super.getStorageConfigurationService().getConfiguration(environment.getRepositoryType());
    }
}


