package org.av360.maverick.graph.store.rdf4j.repository;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.store.IndividualsStore;
import org.av360.maverick.graph.store.rdf.fragments.Fragment;
import org.av360.maverick.graph.store.rdf4j.repository.util.AbstractStore;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j(topic = "graph.repo.entities")
@Component
public class EntityStoreImpl extends AbstractStore implements IndividualsStore {

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


    @Override
    public Mono<Transaction> insertFragment(Fragment fragment, Environment environment) {
        return null;
    }
}


