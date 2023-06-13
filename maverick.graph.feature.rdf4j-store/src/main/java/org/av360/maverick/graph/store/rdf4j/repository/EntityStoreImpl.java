package org.av360.maverick.graph.store.rdf4j.repository;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.store.EntityStore;
import org.av360.maverick.graph.store.rdf.fragments.RdfEntity;
import org.av360.maverick.graph.store.rdf4j.repository.util.AbstractStore;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.ModelCollector;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.slf4j.Logger;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.stream.Stream;

@Slf4j(topic = "graph.repo.entities")
@Component
public class EntityStoreImpl extends AbstractStore implements EntityStore {

    @org.springframework.beans.factory.annotation.Value("${application.storage.entities.path:#{null}}")
    private String path;

    public EntityStoreImpl() {
        super(RepositoryType.ENTITIES);
    }


    public Mono<RdfEntity> getEntity(Resource id, SessionContext ctx, GrantedAuthority requiredAuthority) {
        return this.getEntity(id, ctx, requiredAuthority, 1);
    }

    public Mono<RdfEntity> getEntity(Resource id, SessionContext ctx, GrantedAuthority requiredAuthority, int includeNeighborsLevel) {
        return this.applyWithConnection(ctx, requiredAuthority, connection -> {
            log.trace("Loading entity with id '{}' from repository {}", id, connection.getRepository().toString());

            try (RepositoryResult<Statement> statements = connection.getStatements(id, null, null)) {
                if (!statements.hasNext()) {
                    if (log.isDebugEnabled()) log.debug("Found no statements for IRI: <{}>.", id);
                    return null;
                }

                RdfEntity entity = new RdfEntity(id).withResult(statements);

                if (includeNeighborsLevel >= 1) {
                    HashSet<Value> objects = new HashSet<>(entity.getModel().objects());
                    Stream<RepositoryResult<Statement>> repositoryResultStream = objects.stream()
                            .filter(Value::isIRI)
                            .map(value -> connection.getStatements((IRI) value, null, null));

                    Model neighbours = objects.stream()
                            .filter(Value::isIRI)
                            .map(value -> connection.getStatements((IRI) value, null, null))
                            .flatMap(result -> result.stream())
                            .filter(sts -> sts.getObject().isLiteral())
                            .filter(sts -> sts.getObject().isLiteral() && sts.getObject().stringValue().length() < 50)
                            .collect(new ModelCollector());
                    // ModelCollector
                    entity.getModel().addAll(neighbours);

                }
                // embedded level 1

                if (log.isDebugEnabled())
                    log.debug("Loaded {} statements for entity with IRI: <{}>.", entity.getModel().size(), id);
                return entity;
            } catch (Exception e) {
                log.error("Unknown error while collection statements for entity '{}' ", id, e);
                throw e;
            }
        });
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


