package org.av360.maverick.graph.store.rdf4j.repository.util;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.model.errors.InsufficientPrivilegeException;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.model.vocabulary.Transactions;
import org.av360.maverick.graph.store.RepositoryBuilder;
import org.av360.maverick.graph.store.behaviours.*;
import org.av360.maverick.graph.store.rdf.fragments.RdfEntity;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.av360.maverick.graph.store.rdf.helpers.RdfUtils;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.util.ModelCollector;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.base.RepositoryConnectionWrapper;
import org.eclipse.rdf4j.repository.util.RDFInserter;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFParserFactory;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.function.ThrowingConsumer;
import org.springframework.util.function.ThrowingFunction;
import org.springframework.web.client.HttpClientErrorException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("FieldCanBeLocal")
public abstract class AbstractStore implements TripleStore, StatementsAware, ModelAware, Maintainable, FragmentsAware {

    private final RepositoryType repositoryType;
    private RepositoryBuilder repositoryConfiguration;
    private MeterRegistry meterRegistry;
    private Counter transactionsMonoCounter;
    private Counter transactionsFluxCounter;
    private Timer transactionsMonoTimer;
    private Timer transactionsFluxTimer;

    public AbstractStore(RepositoryType repositoryType) {
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

    @Autowired
    private void setMeterRegistry(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.transactionsMonoCounter = meterRegistry.counter("graph.store.transactions", "cardinality", "single");
        this.transactionsMonoTimer = meterRegistry.timer("graph.store.timer", "cardinality", "single");
        this.transactionsFluxCounter = meterRegistry.counter("graph.store.transactions", "cardinality", "multiple");
        this.transactionsFluxTimer = meterRegistry.timer("graph.store.timer", "cardinality", "multiple");
    }


    public Flux<AnnotatedStatement> construct(String query, Environment environment) {
        return this.applyManyWithConnection(environment, connection -> {
            try {
                getLogger().debug("Running construct query in repository: {}", connection.getRepository());
                getLogger().trace("Query: {}", query.replace('\n', ' ').trim());
                GraphQuery q = connection.prepareGraphQuery(QueryLanguage.SPARQL, query);
                try (GraphQueryResult result = q.evaluate()) {
                    Set<Namespace> namespaces = result.getNamespaces().entrySet().stream()
                            .map(entry -> new SimpleNamespace(entry.getKey(), entry.getValue()))
                            .collect(Collectors.toSet());
                    return result.stream().map(statement -> AnnotatedStatement.wrap(statement, namespaces)).collect(Collectors.toSet());
                } catch (Exception e) {
                    getLogger().warn("Error while running value query.", e);
                    throw e;
                }
            } catch (MalformedQueryException e) {
                getLogger().warn("Error while parsing query", e);
                throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Invalid query");
            } catch (Exception e) {
                getLogger().error("Unknown error while running query", e);
                throw e;
            }
        });
    }

    public Flux<BindingSet> query(String query, Environment environment) {
        return this.applyManyWithConnection(environment, connection -> {
            try {

                getLogger().debug("Running select query in repository: {}", connection.getRepository());
                getLogger().trace("Query: {} ", query.replace('\n', ' ').trim());

                TupleQuery q = connection.prepareTupleQuery(QueryLanguage.SPARQL, query);

                try (TupleQueryResult result = q.evaluate()) {
                    Set<BindingSet> collect = result.stream().collect(Collectors.toSet());
                    if (getLogger().isTraceEnabled())
                        getLogger().trace("Query resulted in {} bindings in repository '{}'", collect.size(), connection.getRepository());
                    return collect;
                } finally {
                    connection.close();
                }

            } catch (MalformedQueryException e) {
                getLogger().warn("Error while parsing query, reason: {}", e.getMessage());
                throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Invalid query");
            } catch (Exception e) {
                getLogger().error("Unknown error while running query", e);
                throw e;
            }
        });
    }

    @Override
    public Mono<Void> reset(Environment environment) {

        return this.consumeWithConnection(environment, connection -> {
            try {
                if (!connection.isOpen() || connection.isActive()) return;

                if (getLogger().isTraceEnabled()) {
                    getLogger().trace("Removing {} statements from repository '{}'", connection.size(), connection.getRepository());
                }

                connection.clear();

                if (!connection.isEmpty())
                    throw new RepositoryException("Repository not empty after clearing");

            } catch (Exception e) {
                getLogger().error("Failed to clear repository: {}", connection.getRepository());
                throw e;
            }
        }).then(getBuilder().shutdownRepository(this, environment)).then();
    }


    @Override
    public Mono<Void> deleteModel(Model model, Environment environment) {
        return this.consumeWithConnection(environment, connection -> {
            try {
                Resource[] contexts = model.contexts().toArray(new Resource[0]);
                connection.remove(model, contexts);
                connection.commit();
                getLogger().trace("Deleted {} statements from repository '{}'", model.size(), connection.getRepository());
            } catch (Exception e) {
                getLogger().error("Error while deleting {} statements from repository '{}'", model.size(), connection.getRepository());
                connection.rollback();
                throw e;
            }
        });
    }


    private InputStream getInputStreamFromFluxDataBuffer(Publisher<DataBuffer> data) throws IOException {
        PipedOutputStream osPipe = new PipedOutputStream();
        PipedInputStream isPipe = new PipedInputStream(osPipe);

        DataBufferUtils.write(data, osPipe)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnComplete(() -> {
                    try {
                        getLogger().trace("Finished reading data buffers from publisher during import of data.");
                        osPipe.close();   // the piped input stream has to be closed by its consumer

                    } catch (IOException ignored) {
                        getLogger().error("Failed to close output stream");
                    }
                })
                .doOnSubscribe(subscription -> getLogger().trace("Starting reading data buffers from publisher during import of data."))
                .subscribe(DataBufferUtils.releaseConsumer());
        return isPipe;
    }

    @Override
    public Mono<Void> importStatements(Publisher<DataBuffer> bytesPublisher, String mimetype, Environment environment) {

        Optional<RDFParserFactory> parserFactory = RdfUtils.getParserFactory(MimeType.valueOf(mimetype));
        Assert.isTrue(parserFactory.isPresent(), "Unsupported mimetype for parsing the file.");

        RDFParser parser = parserFactory.orElseThrow().getParser();

        return this.consumeWithConnection(environment, connection -> {
            try {
                // example: https://www.baeldung.com/spring-reactive-read-flux-into-inputstream
                // solution: https://manhtai.github.io/posts/flux-databuffer-to-inputstream/
                getLogger().trace("Starting to parse input stream with mimetype {}", mimetype);

                RDFInserter rdfInserter = new RDFInserter(connection);
                parser.setRDFHandler(rdfInserter);

                try (InputStream stream = getInputStreamFromFluxDataBuffer(bytesPublisher)) {
                    parser.parse(stream);
                }

                getLogger().trace("Parsing completed into repository '{}'", connection.getRepository().toString());


            } catch (Exception exception) {
                getLogger().error("Failed to import statements with mimetype {} with reason: ", mimetype, exception);
                throw exception;
            }
        });

    }

    public Flux<IRI> types(Resource subj, Environment environment) {
        return this.applyManyWithConnection(environment, connection ->
                connection.getStatements(subj, RDF.TYPE, null, false).stream()
                        .map(Statement::getObject)
                        .filter(Value::isIRI)
                        .map(value -> (IRI) value)
                        .collect(Collectors.toSet()));
    }


    @Override
    public Flux<Transaction> commit(final Collection<Transaction> transactions, Environment environment, boolean merge) {
        return this.applyManyWithConnection(environment, connection -> {
            connection.begin();

            if (merge) {
                RdfTransaction merged = new RdfTransaction();
                transactions.forEach(rdfTransaction -> {
                    merged.getModel().addAll(rdfTransaction.get());
                });
                transactions.clear();
                transactions.add(merged);
            }


            Set<Transaction> result = transactions.stream().peek(trx -> {
                synchronized (connection) {
                    getLogger().trace("Committing transaction '{}' to repository '{}'", trx.getIdentifier().getLocalName(), connection.getRepository().toString());
                    // FIXME: the approach based on the context works only as long as the statements in the graph are all within the global context only
                    // with this approach, we cannot insert a statement to a context (since it is already in GRAPH_CREATED), every st can only be in one context
                    ValueFactory vf = connection.getValueFactory();
                    Model insertStatements = trx.get(Transactions.GRAPH_CREATED);
                    Model removeStatements = trx.get(Transactions.GRAPH_DELETED);

                    try {
                        if (insertStatements.size() > 0) {
                            connection.add(insertStatements);
                        }
                        if (removeStatements.size() > 0) {
                            connection.remove(removeStatements);

                        }
                        if (insertStatements.size() > 0 || removeStatements.size() > 0) {
                            connection.prepare();
                            connection.commit();
                            getLogger().debug("Transaction '{}' completed with {} inserted statements and {} removed statements in repository '{}'.", trx.getIdentifier().getLocalName(), insertStatements.size(), removeStatements.size(), connection.getRepository());

                        }

                        trx.setCompleted();
                    } catch (Exception e) {
                        getLogger().error("Failed to complete transaction for repository '{}'.", connection.getRepository(), e);
                        getLogger().trace("Insert Statements in this transaction: \n {}", insertStatements);
                        getLogger().trace("Remove Statements in this transaction: \n {}", removeStatements);
                        connection.rollback();
                        trx.setFailed(e.getMessage());
                    }
                }

            }).collect(Collectors.toSet());
            connection.close();
            return result;
        });


    }

    @Override
    public Mono<RdfEntity> getFragment(Resource id, int includeNeighborsLevel, Environment environment) {
        return this.applyWithConnection(environment, connection -> {
            getLogger().trace("Loading fragment with id '{}' from repository {}", id, connection.getRepository().toString());

            try (RepositoryResult<Statement> statements = connection.getStatements(id, null, null)) {
                if (!statements.hasNext()) {
                    if (getLogger().isDebugEnabled()) getLogger().debug("Found no statements for IRI: <{}>.", id);
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

                if (getLogger().isDebugEnabled())
                    getLogger().debug("Loaded {} statements for entity with IRI: <{}>.", entity.getModel().size(), id);
                return entity;
            } catch (Exception e) {
                getLogger().error("Unknown error while collection statements for entity '{}' ", id, e);
                throw e;
            }
        });
    }

    @Override
    public Mono<Void> insertModel(Model model, Environment environment) {
        return this.consumeWithConnection(environment, connection -> {
            try {
                if (getLogger().isTraceEnabled())
                    getLogger().trace("Inserting model without transaction to repository '{}'", connection.getRepository().toString());

                Resource[] contexts = model.contexts().toArray(new Resource[0]);
                connection.add(model, contexts);
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        });

    }

    @Override
    public Mono<Transaction> insertModel(Model model, Transaction transaction) {
        Assert.notNull(transaction, "Transaction cannot be null");
        if (getLogger().isTraceEnabled())
            getLogger().trace("Insert planned for {} statements in transaction '{}'.", model.size(), transaction.getIdentifier().getLocalName());

        transaction = transaction
                .inserts(model)
                .affects(model);

        return Mono.just(transaction);
    }


    @Override
    public Mono<Set<Statement>> listStatements(Resource value, IRI predicate, Value object, Environment environment) {
        return this.applyWithConnection(environment, connection -> {
            if (getLogger().isTraceEnabled()) {
                getLogger().trace("Listing all statements with pattern [{},{},{}] from repository '{}'", value, predicate, object, connection.getRepository().toString());
            }

            Set<Statement> statements = connection.getStatements(value, predicate, object).stream().collect(Collectors.toUnmodifiableSet());
            return statements;
        });

    }

    @Override
    public Mono<Boolean> hasStatement(Resource value, IRI predicate, Value object, Environment environment) {
        return this.applyWithConnection(environment, connection -> connection.hasStatement(value, predicate, object, false));

    }


    @Override
    public Mono<Boolean> exists(Resource subj, Environment environment) {
        return this.applyWithConnection(environment, connection -> connection.hasStatement(subj, RDF.TYPE, null, false));
    }


    @Override
    public Mono<Transaction> removeStatements(Collection<Statement> statements, Transaction transaction) {
        Assert.notNull(transaction, "Transaction cannot be null");
        if (getLogger().isTraceEnabled())
            getLogger().trace("Removal planned for {} statements in transaction '{}'.", statements.size(), transaction.getIdentifier().getLocalName());

        return Mono.just(transaction.removes(statements));
    }

    @Override
    public Mono<Transaction> addStatement(Resource subject, IRI predicate, Value literal, Transaction transaction) {
        return this.addStatement(subject, predicate, literal, null, transaction);
    }

    @Override
    public Mono<Transaction> addStatement(Resource subject, IRI predicate, Value literal, @Nullable Resource context, Transaction transaction) {
        if (getLogger().isTraceEnabled())
            getLogger().trace("Marking statement for insert in transaction {}: {} - {} - {}", transaction.getIdentifier().getLocalName(), subject, predicate, literal);


        Transaction trx = transaction
                .inserts(subject, predicate, literal, context)
                .affects(subject, predicate, literal);
        return Mono.just(trx);
    }


    protected <T> Mono<T> applyWithConnection(Environment environment, ThrowingFunction<RepositoryConnection, T> fun) {
        return transactionsMonoTimer.record(() ->
                this.verifyValidAndAuthorized(environment)
                        //.then(this.assertPrivilege(environment))
                        .then(this.getBuilder().buildRepository(this, environment))
                        .flatMap(repository -> {
                            try (RepositoryConnection connection = repository.getConnection()) {

                                T result = fun.applyWithException(new RepositoryConnectionWrapper(repository, connection));
                                if (Objects.isNull(result)) return Mono.empty();
                                else return Mono.just(result);
                            } catch (Exception e) {
                                return Mono.error(e);
                            } finally {
                                transactionsMonoCounter.increment();
                            }
                        }));
    }


    protected Mono<Void> consumeWithConnection(Environment environment, ThrowingConsumer<RepositoryConnection> fun) {
        return transactionsMonoTimer.record(() ->
                this.verifyValidAndAuthorized(environment)
                        // .then(this.assertPrivilege(ctx, requiredAuthority))
                        .flatMap(env -> this.getBuilder().buildRepository(this, env))
                        .switchIfEmpty(Mono.error(new IOException("Failed to build repository for repository of type: " + this.getRepositoryType())))
                        .flatMap(repository -> {
                            try (RepositoryConnection connection = repository.getConnection()) {

                                fun.acceptWithException(new RepositoryConnectionWrapper(repository, connection));
                                return Mono.empty();
                            } catch (Exception e) {
                                return Mono.error(e);
                            } finally {
                                transactionsMonoCounter.increment();
                            }
                        }));
    }

    private Mono<Environment> verifyValidAndAuthorized(Environment environment) {
        try {
            environment.withRepositoryType(this.getRepositoryType());
            if (!environment.isAuthorized()) {
                throw new InsufficientPrivilegeException("Unauthorized operation in environment '%s'".formatted(environment));
            }
            this.validateEnvironment(environment);
            return Mono.just(environment);
        } catch (Exception e) {
            return Mono.error(e);
        }


    }

    protected void validateEnvironment(Environment environment) {
        // do nothing
    }

    @Override
    public Mono<Model> getModel(Environment environment) {
        return this.listStatements(null, null, null, environment).map(LinkedHashModel::new);
    }

    protected <E, T extends Iterable<E>> Flux<E> applyManyWithConnection(Environment environment, ThrowingFunction<RepositoryConnection, T> fun) {

        Flux<E> result =
                this.verifyValidAndAuthorized(environment)
                        // .then(this.assertPrivilege(environment, requiredAuthority))
                        .then(this.getBuilder().buildRepository(this, environment))
                        .flatMapMany(repository -> {
                            try (RepositoryConnection connection = repository.getConnection()) {
                                return Flux.fromIterable(fun.apply(connection));
                            } catch (Exception e) {
                                this.meterRegistry.counter("graph.store.operations", "cardinality", "multiple", "state", "failure").increment();
                                getLogger().warn("Error while applying function to repository '{}' with message '{}'. Active connections for repository: {}", repository, e.getMessage(), repository.getConnectionsCount());
                                return Mono.error(e);
                            } finally {
                                this.meterRegistry.counter("graph.store.operations", "cardinality", "multiple", "state", "complete").increment();
                            }
                        });
                        // .doOnSubscribe(subscription -> getLogger().trace("Applying function with many results."));

        // FIXME: should check whether we are called from a scheduler
        if (environment.getSessionContext().isScheduled()) {
            result.timeout(Duration.ofMillis(10000))
                    .onErrorResume(throwable -> {
                        if (throwable instanceof TimeoutException te) {
                            getLogger().warn("Long-running operation on repository of type '{}' has been canceled.", repositoryType);
                            return Flux.error(new TimeoutException("Timeout while applying operation to repository:" + repositoryType.toString()));
                        } else {
                            return Flux.error(throwable);
                        }
                    });
        }
        return result;

    }

    private Mono<Void> assertPrivilege(SessionContext ctx, GrantedAuthority requiredAuthority) {
        if (Objects.isNull(requiredAuthority)) {
            return Mono.error(new UnsupportedOperationException("Missing required authority while access a repository."));
        }

        if (ctx.getAuthentication().isPresent()) {
            if (!Authorities.satisfies(requiredAuthority, ctx.getAuthentication().get().getAuthorities())) {
                String msg = String.format("Required authority '%s' for repository '%s' not met in ctx with authorities '%s'", requiredAuthority.getAuthority(), repositoryType.name(), ctx.getAuthentication().get().getAuthorities());
                return Mono.error(new InsufficientPrivilegeException(msg));
            } else return Mono.empty();
        } else {
            return Mono.error(new UnsupportedOperationException("Missing required authentication while access a repository."));
        }

    }


}