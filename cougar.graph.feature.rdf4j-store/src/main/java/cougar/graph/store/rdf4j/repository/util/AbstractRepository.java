package cougar.graph.store.rdf4j.repository.util;

import cougar.graph.model.enums.Activity;
import cougar.graph.model.rdf.NamespaceAwareStatement;
import cougar.graph.store.rdf.models.Transaction;
import cougar.graph.model.vocabulary.Transactions;
import cougar.graph.store.RepositoryBuilder;
import cougar.graph.store.RepositoryType;
import cougar.graph.store.behaviours.ModelUpdates;
import cougar.graph.store.behaviours.RepositoryBehaviour;
import cougar.graph.store.behaviours.Resettable;
import cougar.graph.store.behaviours.Statements;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.util.Assert;
import org.springframework.web.client.HttpClientErrorException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j(topic = "cougar.graph.repository")
public class AbstractRepository implements RepositoryBehaviour, Statements, ModelUpdates, Resettable {

    private final RepositoryType repositoryType;
    private RepositoryBuilder repositoryConfiguration;

    public AbstractRepository(RepositoryType repositoryType) {
        this.repositoryType = repositoryType;
    }


    public RepositoryType getRepositoryType() {
        return this.repositoryType;
    }

    @Override
    public RepositoryBuilder getBuilder() {
        return this.repositoryConfiguration;
    }

    @Autowired
    private void setConfiguration(RepositoryBuilder repositoryConfiguration) {
        this.repositoryConfiguration = repositoryConfiguration;
    }


    @Deprecated
    public Mono<Void> store(Model model, Authentication authentication) {
        try (RepositoryConnection connection = this.getConnection(authentication)) {
            try {
                Resource[] contexts = model.contexts().toArray(new Resource[model.contexts().size()]);
                connection.add(model, contexts);
                connection.commit();
                log.debug("Model was stored in repository '{}'", connection.getRepository().toString());
                return Mono.empty();
            } catch (Exception e) {
                connection.rollback();
                return Mono.error(e);
            }
        } catch (Exception e) {
            log.error("Failed to initialize repository connection");
            return Mono.error(e);
        }
    }


    public Flux<NamespaceAwareStatement> construct(String query, Authentication authentication) {
        return Flux.create(c -> {
            try (RepositoryConnection connection = getConnection(authentication)) {
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

    public Flux<BindingSet> query(String query, Authentication authentication) {
        return Flux.create(emitter -> {
            try (RepositoryConnection connection = this.getConnection(authentication)) {

                TupleQuery q = connection.prepareTupleQuery(QueryLanguage.SPARQL, query);
                if (log.isTraceEnabled())
                    log.trace("Querying repository '{}' with SPQARL Query: {}", connection.getRepository(), query.replace('\n', ' ').trim());
                try (TupleQueryResult result = q.evaluate()) {
                    result.stream().forEach(emitter::next);
                } finally {
                    emitter.complete();
                }

            } catch (MalformedQueryException e) {
                log.warn("Error while parsing query, reason: {}", e.getMessage());
                emitter.error(e);
            } catch (Exception e) {
                log.error("Unknown error while running query", e);
                emitter.error(e);
            }
        });
    }

    @Override
    public Mono<Void> reset(Authentication authentication, RepositoryType repositoryType) {
        try (RepositoryConnection connection = getConnection(authentication, repositoryType)) {
            if (log.isTraceEnabled()) log.trace("Resetting repository '{}'", connection.getRepository());
            RepositoryResult<Statement> statements = connection.getStatements(null, null, null);
            connection.remove(statements);
            return Mono.empty();
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    @Override
    public Flux<Transaction> commit(Collection<Transaction> transactions, Authentication authentication) {
        return Flux.create(c -> {
            try (RepositoryConnection connection = this.getConnection(authentication)) {


                log.trace("(Store) Committing transaction to repository '{}'", connection.getRepository().toString());

                transactions.forEach(trx -> {
                    // FIXME: the approach based on the context works only as long as the statements in the graph are all within the global context only
                    // with this approach, we cannot insert a statement to a context (since it is already in GRAPH_CREATED), every st can only be in one context
                    Model insertModel = trx.getModel().filter(null, null, null, Transactions.GRAPH_CREATED);
                    Model removeModel = trx.getModel().filter(null, null, null, Transactions.GRAPH_DELETED);

                    // we have to get rid of the context
                    SimpleValueFactory vf = SimpleValueFactory.getInstance();
                    List<Statement> insertStatements = insertModel.stream().map(s -> vf.createStatement(s.getSubject(), s.getPredicate(), s.getObject())).toList();
                    List<Statement> removeStatements = removeModel.stream().map(s -> vf.createStatement(s.getSubject(), s.getPredicate(), s.getObject())).toList();

                    try {
                        connection.begin();
                        connection.add(insertStatements);
                        connection.remove(removeStatements);
                        connection.commit();

                        trx.setCompleted();

                        log.trace("(Store) Transaction completed with {} inserted statements and {} removed statements in repository '{}'.", insertModel.size(), removeModel.size(), connection.getRepository());
                        c.next(trx);
                    } catch (Exception e) {
                        log.error("(Store) Failed to complete transaction for repository '{}'.", connection.getRepository(), e);
                        log.trace("(Store) Insert Statements in this transaction: \n {}", insertModel);
                        log.trace("(Store) Remove Statements in this transaction: \n {}", removeModel);

                        connection.rollback();
                        trx.setFailed(e.getMessage());
                        c.next(trx);
                    }


                });

                c.complete();
            } catch (Exception e) {
                log.error("Failed to initialize repository connection");
                c.error(e);
            }
        });
    }


    @Override
    public Mono<Void> insert(Model model, Authentication authentication) {

        try (RepositoryConnection connection = this.getConnection(authentication)) {
            try {

                if (log.isTraceEnabled())
                    log.trace("(Store) Inserting model without transaction to repository '{}'", connection.getRepository().toString());

                Resource[] contexts = model.contexts().toArray(new Resource[model.contexts().size()]);
                connection.add(model, contexts);
                connection.commit();
                return Mono.empty();
            } catch (Exception e) {
                connection.rollback();
                return Mono.error(e);
            }
        } catch (RepositoryException e) {
            return Mono.error(e);
        } catch (IOException e) {
            return Mono.error(e);
        }

    }

    @Override
    public Mono<List<Statement>> listStatements(Resource value, IRI predicate, Value object, Authentication authentication) {
        try (RepositoryConnection connection = getConnection(authentication)) {
            if (log.isTraceEnabled())
                log.trace("(Store) Listing all statements with pattern [{},{},{}] from repository '{}'", value, predicate, object, connection.getRepository().toString());

            List<Statement> statements = connection.getStatements(value, predicate, object).stream().toList();
            return Mono.just(statements);
        } catch (Exception e) {
            return Mono.error(e);
        }

    }

    @Override
    public Mono<Transaction> removeStatements(Collection<Statement> statements, Transaction transaction) {
        Assert.notNull(transaction, "Transaction cannot be null");

        statements.forEach(statement -> transaction.remove(statement, Activity.REMOVED).affected(statement));
        return transaction.asMono();
    }

    @Override
    public Mono<Transaction> addStatement(Resource subject, IRI predicate, Value literal, Transaction transaction) {
        Assert.notNull(transaction, "Transaction cannot be null");

        return transaction
                .insert(subject, predicate, literal, Activity.UPDATED)
                .affected(subject, predicate, literal)
                .asMono();

    }


}