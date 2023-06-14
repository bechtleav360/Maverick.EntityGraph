package org.av360.maverick.graph.feature.applications.store.rdf4j;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.applications.store.ApplicationsStore;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.store.rdf4j.repository.util.AbstractStore;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

/**
 *
 */
@Component
@Slf4j(topic = "graph.repo.applications")
public class ApplicationsRepository extends AbstractStore implements ApplicationsStore {

    @Value("${application.storage.system.path:#{null}}")
    private String path;
    public ApplicationsRepository() {
        super(RepositoryType.APPLICATION);
    }


    @Override
    public Logger getLogger() {
        return log;
    }

    @Override
    public String getDirectory() {
        if(StringUtils.hasLength(this.path)) return this.path+"/applications";
        else return "";
    }

    @Override
    protected Mono<SessionContext> validateContext(SessionContext ctx) {
        return Mono.just(ctx.withEnvironment().setRepositoryType(RepositoryType.APPLICATION));
    }
}
