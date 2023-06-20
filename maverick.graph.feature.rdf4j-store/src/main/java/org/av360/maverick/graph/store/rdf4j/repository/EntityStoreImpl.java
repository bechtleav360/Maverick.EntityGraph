package org.av360.maverick.graph.store.rdf4j.repository;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.store.EntityStore;
import org.av360.maverick.graph.store.rdf4j.repository.util.AbstractStore;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

@Slf4j(topic = "graph.repo.entities")
@Component
public class EntityStoreImpl extends AbstractStore implements EntityStore {

    @org.springframework.beans.factory.annotation.Value("${application.storage.entities.path:#{null}}")
    private String path;

    public EntityStoreImpl() {
        super(RepositoryType.ENTITIES);
    }






    @Override
    public Logger getLogger() {
        return log;
    }

    @Override
    public String getDirectory() {
        return this.path;
    }


}


