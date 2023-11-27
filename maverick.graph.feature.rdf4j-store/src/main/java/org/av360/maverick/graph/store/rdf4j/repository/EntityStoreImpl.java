package org.av360.maverick.graph.store.rdf4j.repository;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.store.IndividualsStore;
import org.av360.maverick.graph.store.rdf.fragments.RdfFragment;
import org.av360.maverick.graph.store.rdf4j.repository.util.AbstractRdfRepository;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j(topic = "graph.repo.entities")
@Component
public class EntityStoreImpl extends AbstractRdfRepository implements IndividualsStore {

    @org.springframework.beans.factory.annotation.Value("${application.storage.entities.path:#{null}}")
    private String path;

    @Override
    public Logger getLogger() {
        return log;
    }

    @Override
    public String getDirectory() {
        return this.path;
    }


    @Override
    public Mono<Transaction> insertFragment(RdfFragment fragment, Environment environment) {
        return null;
    }
}


