package io.av360.maverick.graph.store.rdf4j.repository;

import io.av360.maverick.graph.store.EntityStore;
import io.av360.maverick.graph.store.RepositoryType;
import io.av360.maverick.graph.store.rdf.models.Entity;
import io.av360.maverick.graph.store.rdf4j.repository.util.AbstractRepository;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j(topic = "graph.repo.entities")
@Component
public class EntityRepository extends AbstractRepository implements EntityStore {


    public EntityRepository() {
        super(RepositoryType.ENTITIES);
    }


    public Mono<Entity> getEntity(Resource id, Authentication authentication, GrantedAuthority requiredAuthority) {
        try (RepositoryConnection connection = getConnection(authentication, requiredAuthority)) {
            log.trace("Loading entity with id '{}' from repository {}", id, connection.getRepository().toString());

            RepositoryResult<Statement> statements = connection.getStatements(id, null, null);
            if (!statements.hasNext()) {
                if (log.isDebugEnabled()) log.debug("(Store) Found no statements for IRI: <{}>.", id);
                return Mono.empty();
            }


            Entity entity = new Entity().withResult(statements);

            // embedded level 1
            entity.getModel().objects().stream()
                    .filter(Value::isIRI)
                    .map(value -> connection.getStatements((IRI) value, null, null))
                    .toList()
                    .forEach(entity::withResult);


            if (log.isDebugEnabled())
                log.debug("Loaded {} statements for entity with IRI: <{}>.", entity.getModel().size(), id);
            return Mono.just(entity);

        } catch (Exception e) {
            log.error("Unknown error while collection statements for entity '{}' ", id,  e);
            return Mono.error(e);
        }
    }










}
