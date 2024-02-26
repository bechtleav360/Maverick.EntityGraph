package org.av360.maverick.graph.store.rdf4j.repository.util;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.av360.maverick.graph.model.context.Environment;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.entities.Transaction;
import org.av360.maverick.graph.model.errors.InsufficientPrivilegeException;
import org.av360.maverick.graph.model.errors.store.InvalidStoreConfiguration;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.model.vocabulary.meg.Local;
import org.av360.maverick.graph.model.vocabulary.meg.Transactions;
import org.av360.maverick.graph.store.FragmentsStore;
import org.av360.maverick.graph.store.RepositoryBuilder;
import org.av360.maverick.graph.store.behaviours.*;
import org.av360.maverick.graph.store.rdf.fragments.RdfFragment;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.av360.maverick.graph.store.rdf.fragments.TripleModel;
import org.av360.maverick.graph.store.rdf.helpers.RdfUtils;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.util.ModelCollector;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
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
public abstract class AbstractRdfRepository implements Searchable, Maintainable, Selectable, StatementsAware, Fragmentable, TripleStore, FragmentsStore {

    private RepositoryBuilder repositoryConfiguration;
    private MeterRegistry meterRegistry;
    private Counter transactionsMonoCounter;
    private Counter transactionsFluxCounter;
    private Timer transactionsMonoTimer;
    private Timer transactionsFluxTimer;

    public AbstractRdfRepository() {

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
    private void setMeterRegistry(@Nullable MeterRegistry meterRegistry) {
        if (Objects.nonNull(meterRegistry)) {
            this.meterRegistry = meterRegistry;
            this.transactionsMonoCounter = meterRegistry.counter("graph.store.transactions", "cardinality", "single");
            this.transactionsMonoTimer = meterRegistry.timer("graph.store.timer", "cardinality", "single");
            this.transactionsFluxCounter = meterRegistry.counter("graph.store.transactions", "cardinality", "multiple");
            this.transactionsFluxTimer = meterRegistry.timer("graph.store.timer", "cardinality", "multiple");
        }

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
                    return result.stream().map(statement -> AnnotatedStatement.wrap(statement, namespaces));
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

    @Override
    public Mono<Long> countFragments(Environment environment) {
        return this.applyWithConnection(environment, connection -> {
            try {
                long count = connection.getStatements(null, null, Local.Entities.TYPE_INDIVIDUAL).stream().count();
                return count;
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

                // iterator -> stream -> flux: when the flux completes, the stream closes and as such also the query result
                TupleQueryResult result = q.evaluate();
                Stream<BindingSet> stream = result.stream();
                if (getLogger().isTraceEnabled())
                    getLogger().trace("Query resulted in bindings [{}] in repository '{}'", result.getBindingNames(), connection.getRepository());
                return stream;

            } catch (MalformedQueryException e) {
                getLogger().warn("Error while parsing query, reason: {}", e.getMessage());
                throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Invalid query");
            } catch (Exception e) {
                getLogger().error("Unknown error while running query", e);
                throw e;
            }
        });
    }


    public Mono<Void> update(String query, Environment environment) {
        return this.consumeWithConnection(environment, connection -> {
            try {

                getLogger().debug("Running update query in repository: {}", connection.getRepository());
                getLogger().trace("Query: {} ", query.replace('\n', ' ').trim());

                Update q = connection.prepareUpdate(QueryLanguage.SPARQL, query);
                q.setMaxExecutionTime(30);
                q.execute();

                if (getLogger().isInfoEnabled())
                    getLogger().info("Update query executed");

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
    public Mono<Void> purge(Environment environment) {

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


    public Flux<IRI> subjects(@Nullable IRI type, Environment environment) {
        return this.applyManyWithConnection(environment, connection ->
                connection.getStatements(null, RDF.TYPE, type, false).stream()
                        .map(Statement::getSubject)
                        .filter(Value::isIRI)
                        .map(value -> (IRI) value)
        );
    }


    public Flux<IRI> types(Resource subj, Environment environment) {
        return this.applyManyWithConnection(environment, connection ->
                connection.getStatements(subj, RDF.TYPE, null, false).stream()
                        .map(Statement::getObject)
                        .filter(Value::isIRI)
                        .map(value -> (IRI) value)
        );
    }


    @Override
    public Flux<Transaction> commit(final Collection<Transaction> transactions, Environment environment, boolean merge) {
        return this.applyManyWithConnection(environment, connection -> {

            if (merge) {
                RdfTransaction merged = new RdfTransaction();
                transactions.forEach(rdfTransaction -> {
                    merged.getModel().addAll(rdfTransaction.getModel());
                });
                transactions.clear();
                transactions.add(merged);
            }


            Stream<Transaction> result = transactions.stream().peek(trx -> {
                synchronized (connection) {
                    getLogger().trace("Committing transaction '{}' to repository '{}'", trx.getIdentifier().getLocalName(), connection.getRepository().toString());
                    // FIXME: the approach based on the context works only as long as the statements in the graph are all within the global context only
                    // with this approach, we cannot insert a statement to a context (since it is already in GRAPH_CREATED), every st can only be in one context
                    ValueFactory vf = connection.getValueFactory();
                    Model insertStatements = trx.getModel(Transactions.GRAPH_CREATED);
                    Model updateStatements = trx.getModel(Transactions.GRAPH_UPDATED);
                    Model removeStatements = trx.getModel(Transactions.GRAPH_DELETED);


                    // FIXME: Reification
                    insertStatements = Models.convertRDFStarToReification(connection.getValueFactory(), insertStatements);
                    updateStatements = Models.convertRDFStarToReification(connection.getValueFactory(), updateStatements);
                    removeStatements = Models.convertRDFStarToReification(connection.getValueFactory(), removeStatements);

                    try {
                        if (removeStatements.size() > 0) {
                            connection.remove(removeStatements);
                        }
                        if (insertStatements.size() > 0) {
                            connection.add(insertStatements);
                        }
                        if (updateStatements.size() > 0) {
                            connection.add(updateStatements);
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

            });
            return result;
        });


    }

    @Override
    public Mono<RdfFragment> getFragment(Resource id, int includeNeighborsLevel, boolean includeDetails, Environment environment) {
        return this.applyWithConnection(environment, connection -> {
            getLogger().trace("Loading fragment with id '{}' from repository {}", id, connection.getRepository().toString());

            try (RepositoryResult<Statement> statements = connection.getStatements(id, null, null)) {
                if (!statements.hasNext()) {
                    if (getLogger().isDebugEnabled()) getLogger().debug("Found no statements for IRI: <{}>.", id);
                    return null;
                }

                RdfFragment entity = new RdfFragment(id).withResult(statements);

                Model embeds = loadEmbeds(connection, entity);
                entity.getModel().addAll(embeds);
                if (includeDetails) {
                    Model details = loadDetailsWithReification(connection, entity);
                    entity.getModel().addAll(details);
                }



                if (includeNeighborsLevel == 1) {
                    Model neighbours = loadNeighbours(connection, entity);
                    entity.getModel().addAll(neighbours);
                }


                if (getLogger().isTraceEnabled())
                    getLogger().trace("Loaded {} statements for entity with IRI: <{}>.", entity.getModel().size(), id);
                return entity;
            } catch (Exception e) {
                getLogger().error("Unknown error while collection statements for entity '{}' ", id, e);
                throw e;
            }
        });
    }



    @Override
    public Flux<RdfFragment> listFragments(IRI type, int limit, int offset, Environment environment) {
        return this.subjects(type, environment).flatMap(subject -> this.getFragment(subject, environment));
    }


    @Override
    public Mono<Transaction> insertFragment(RdfFragment fragment, Environment environment) {
        Transaction trx = new RdfTransaction().forInsert(fragment.listStatements());
        return this.commit(trx, environment);
    }

    /**
     * @param connection
     * @param triples
     * @return
     */
    private Model loadDetails(RepositoryConnection connection, TripleModel triples) {
        Model collect = triples.getModel().stream()
                .filter(statement -> (statement.getObject().isLiteral() || statement.getObject().isIRI()) && statement.getSubject().isIRI())
                .map(Values::triple)
                .flatMap(triple -> connection.getStatements(triple, null, null).stream())
                .collect(new ModelCollector());
        return Models.convertReificationToRDFStar(collect);
    }

    /**
     * @param connection
     * @param triples
     * @return
     * @deprecated Required as long as we don't have native RDF star in the LMDB repository. See https://github.com/eclipse-rdf4j/rdf4j/issues/3723
     * <p>
     * <<ex:bob foaf:age 23>> ex:certainty 0.9 .
     * becomes
     * _:node1 a rdf:Statement;
     * rdf:subject ex:bob ;
     * rdf:predicate foaf:age ;
     * rdf:object 23 ;
     * ex:certainty 0.9 .
     */
    @Deprecated
    private Model loadDetailsWithReification(RepositoryConnection connection, TripleModel triples) {
        Model md = triples.getModel().stream()
                .filter(statement -> (statement.getObject().isLiteral() || statement.getObject().isIRI()) && statement.getSubject().isIRI())
                .flatMap(statement -> connection.getStatements(null, RDF.SUBJECT, statement.getSubject()).stream())
                .flatMap(reification_subject_statement -> connection.getStatements(reification_subject_statement.getSubject(), null, null).stream())
                .collect(new ModelCollector());
        return Models.convertReificationToRDFStar(md);

    }

    private Model loadEmbeds(RepositoryConnection connection, RdfFragment entity) {
        HashSet<Value> objects = new HashSet<>(entity.getModel().objects());

        Set<Resource> embedsSubjects = objects.stream()
                .filter(Value::isIRI)
                .flatMap(value -> connection.getStatements((IRI) value, RDF.TYPE, Local.Entities.TYPE_EMBEDDED).stream())
                .map(Statement::getSubject)
                .collect(Collectors.toSet());

        Model result = embedsSubjects.stream()
                .flatMap(resource -> connection.getStatements(resource, null, null).stream())
                .collect(new ModelCollector());
        return result;
    }

    private Model loadNeighbours(RepositoryConnection connection, RdfFragment entity) {
        HashSet<Value> objects = new HashSet<>(entity.getModel().objects());
        /*Stream<RepositoryResult<Statement>> repositoryResultStream = objects.stream()
                .filter(Value::isIRI)
                .map(value -> connection.getStatements((IRI) value, null, null));*/

        return objects.stream()
                .filter(Value::isIRI)
                .flatMap(value -> connection.getStatements((IRI) value, null, null).stream())
                .filter(sts -> this.isLiteralStatement(sts) || this.isTypeStatement(sts))
                .collect(new ModelCollector());

        // ModelCollector

    }

    private boolean isLiteralStatement(Statement statement) {
        return statement.getObject().isLiteral() && statement.getObject().stringValue().length() < 128;
    }

    private boolean isTypeStatement(Statement statement) {
        return statement.getPredicate().equals(RDF.TYPE);
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


    protected <T> Mono<T> applyWithConnection(Environment environment, ThrowingFunction<RepositoryConnection, T> fun) {
        return transactionsMonoTimer.record(() ->
                this.verifyValidAndAuthorized(environment)
                        .then(this.getBuilder().getRepository(this, environment))
                        .flatMap(repository -> {
                            try {
                                RepositoryConnection connection = repository.getConnection();
                                T result = fun.applyWithException(new RepositoryConnectionWrapper(repository, connection));
                                connection.close();
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
                        .flatMap(env -> this.getBuilder().getRepository(this, env))
                        .switchIfEmpty(Mono.error(new IOException("Failed to build repository for repository of type: " + environment.getRepositoryType())))
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

            if (!environment.isAuthorized()) {
                throw new InsufficientPrivilegeException("Unauthorized operation in environment '%s'".formatted(environment));
            }

            if (Objects.isNull(environment.getRepositoryType())) {
                throw new InvalidStoreConfiguration("Missing repository type in environment, aborting request");
            }
            return Mono.just(environment);
        } catch (Exception e) {
            return Mono.error(e);
        }


    }


    protected <E, T extends Stream<E>> Flux<E> applyManyWithConnection(Environment environment, ThrowingFunction<RepositoryConnection, T> fun) {

        Flux<E> result =
                this.verifyValidAndAuthorized(environment)
                        // .then(this.assertPrivilege(environment, requiredAuthority))
                        .then(this.getBuilder().getRepository(this, environment))
                        .flatMapMany(repository -> {
                            try {
                                RepositoryConnection connection = repository.getConnection();
                                Stream<E> stream = fun.apply(connection);
                                return Flux.fromStream(stream).doOnComplete(connection::close);
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
                            getLogger().warn("Long-running operation on repository of type '{}' has been canceled.", environment.getRepositoryType());
                            return Flux.error(new TimeoutException("Timeout while applying operation to repository:" + environment.getRepositoryType().toString()));
                        } else {
                            return Flux.error(throwable);
                        }
                    });
        }
        return result;

    }

    @Deprecated
    private Mono<Void> assertPrivilege(SessionContext ctx, GrantedAuthority requiredAuthority) {
        if (Objects.isNull(requiredAuthority)) {
            return Mono.error(new UnsupportedOperationException("Missing required authority while access a repository."));
        }

        if (ctx.getAuthentication().isPresent()) {
            if (!Authorities.satisfies(requiredAuthority, ctx.getAuthentication().get().getAuthorities())) {
                String msg = String.format("Required authority '%s' for repository '%s' not met in ctx with authorities '%s'", requiredAuthority.getAuthority(), ctx.getEnvironment().getRepositoryType().name(), ctx.getAuthentication().get().getAuthorities());
                return Mono.error(new InsufficientPrivilegeException(msg));
            } else return Mono.empty();
        } else {
            return Mono.error(new UnsupportedOperationException("Missing required authentication while access a repository."));
        }

    }


}