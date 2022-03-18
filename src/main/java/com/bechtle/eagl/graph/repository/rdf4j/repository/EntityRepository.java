package com.bechtle.eagl.graph.repository.rdf4j.repository;

import com.bechtle.eagl.graph.domain.model.enums.Activity;
import com.bechtle.eagl.graph.domain.model.extensions.NamespaceAwareStatement;
import com.bechtle.eagl.graph.domain.model.wrapper.Entity;
import com.bechtle.eagl.graph.domain.model.wrapper.Transaction;
import com.bechtle.eagl.graph.repository.EntityStore;
import com.bechtle.eagl.graph.repository.rdf4j.config.RepositoryConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.client.HttpClientErrorException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class EntityRepository extends AbstractRepository implements EntityStore {


    public EntityRepository() {
        super(RepositoryConfiguration.RepositoryType.ENTITIES);
    }



    @Override
    public Mono<Entity> get(IRI id) {
        try (RepositoryConnection connection = super.getRepository().getConnection()) {
            log.trace("(Store) Loading entity from repository {}", getRepository().toString());

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
                log.debug("(Store) Loaded {} statements for entity with IRI: <{}>.", entity.getModel().size(), id);
            return Mono.just(entity);

        } catch (Exception e) {
            log.error("Unknown error while running query", e);
            return Mono.error(e);
        }
    }


    @Override
    public Flux<NamespaceAwareStatement> constructQuery(String query) {

        return Flux.create(c -> {
            try (RepositoryConnection connection = getRepository().getConnection()) {
                GraphQuery q = connection.prepareGraphQuery(QueryLanguage.SPARQL, query);
                try (GraphQueryResult result = q.evaluate()) {
                    Set<Namespace> namespaces = result.getNamespaces().entrySet().stream()
                            .map(entry -> new SimpleNamespace(entry.getKey(), entry.getValue()))
                            .collect(Collectors.toSet());

                    result.forEach(statement -> c.next(NamespaceAwareStatement.wrap(statement, namespaces)));
                } catch (Exception e) {
                    log.warn("Error while running value query.", e);
                    c.error(e);
                }
            } catch (MalformedQueryException e) {
                log.warn("Error while parsing query", e);
                c.error(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Invalid query"));
            } catch (Exception e) {
                log.error("Unknown error while running query", e);
                c.error(e);
            } finally {
                c.complete();
            }
        });
    }




    @Override
    public Mono<List<Statement>> listStatements(Resource value, IRI predicate, Value object) {
        try (RepositoryConnection connection = getRepository().getConnection()) {
            List<Statement> statements = connection.getStatements(value, predicate, object).stream().toList();
            return Mono.just(statements);
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    @Override
    public Mono<Transaction> addStatement(Resource subject, IRI predicate, Value value, Transaction transaction) {
        Assert.notNull(transaction, "Transaction cannot be null");

        return transaction
                .insert(subject, predicate, value, Activity.UPDATED)
                .affected(subject, predicate, value)
                .asMono();

    }


    @Override
    public Mono<Transaction> removeStatement(Resource subject, IRI predicate, Value value, Transaction transaction) {
        Assert.notNull(transaction, "Transaction cannot be null");

        return transaction
                .remove(subject, predicate, value, Activity.UPDATED)
                .affected(subject, predicate, value)
                .asMono();
    }

}
