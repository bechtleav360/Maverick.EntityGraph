package org.av360.maverick.graph.store.rdf4j.repository;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.store.EntityStore;
import org.av360.maverick.graph.store.RepositoryType;
import org.av360.maverick.graph.store.rdf.fragments.RdfEntity;
import org.av360.maverick.graph.store.rdf4j.repository.util.AbstractStore;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.slf4j.Logger;
import org.springframework.security.core.Authentication;
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



    public Mono<RdfEntity> getEntity(Resource id, Authentication authentication, GrantedAuthority requiredAuthority) {
        return this.getEntity(id, authentication, requiredAuthority, 1);
    }

    public Mono<RdfEntity> getEntity(Resource id, Authentication authentication, GrantedAuthority requiredAuthority, int includeNeighborsLevel) {
        return this.applyWithConnection(authentication, requiredAuthority, connection -> {
            log.trace("Loading entity with id '{}' from repository {}", id, connection.getRepository().toString());

            try (RepositoryResult<Statement> statements = connection.getStatements(id, null, null)) {
                if (!statements.hasNext()) {
                    if (log.isDebugEnabled()) log.debug("Found no statements for IRI: <{}>.", id);
                    return null;
                }

                RdfEntity entity = new RdfEntity(id).withResult(statements);

                if(includeNeighborsLevel >= 1) {
                    HashSet<Value> objects = new HashSet<>(entity.getModel().objects());

                    Stream<RepositoryResult<Statement>> repositoryResultStream = objects.stream()
                            .filter(Value::isIRI)
                            .map(value -> connection.getStatements((IRI) value, null, null));

                    repositoryResultStream.forEach(entity::withResult);

                }
                // embedded level 1

                if (log.isDebugEnabled())
                    log.debug("Loaded {} statements for entity with IRI: <{}>.", entity.getModel().size(), id);
                return entity;
            } catch (Exception e) {
                log.error("Unknown error while collection statements for entity '{}' ", id,  e);
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
