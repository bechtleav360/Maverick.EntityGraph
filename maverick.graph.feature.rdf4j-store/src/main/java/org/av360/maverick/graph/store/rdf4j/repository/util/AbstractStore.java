package org.av360.maverick.graph.store.rdf4j.repository.util;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.av360.maverick.graph.model.enums.Activity;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.model.vocabulary.Transactions;
import org.av360.maverick.graph.store.RepositoryBuilder;
import org.av360.maverick.graph.store.RepositoryType;
import org.av360.maverick.graph.store.behaviours.ModelUpdates;
import org.av360.maverick.graph.store.behaviours.Resettable;
import org.av360.maverick.graph.store.behaviours.Statements;
import org.av360.maverick.graph.store.behaviours.TripleStore;
import org.av360.maverick.graph.store.rdf.fragments.RdfTransaction;
import org.av360.maverick.graph.store.rdf.helpers.RdfUtils;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.base.RepositoryConnectionWrapper;
import org.eclipse.rdf4j.repository.util.RDFInserter;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFParserFactory;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
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
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public abstract class AbstractStore implements TripleStore, Statements, ModelUpdates, Resettable {

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


    public Flux<AnnotatedStatement> construct(String query, Authentication authentication, GrantedAuthority requiredAuthority) {
        return this.applyManyWithConnection(authentication, requiredAuthority, connection -> {
            try {
                getLogger().trace("Running construct query in repository: {}", connection.getRepository());

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

    public Flux<BindingSet> query(String query, Authentication authentication, GrantedAuthority requiredAuthority) {
        return this.applyManyWithConnection(authentication, requiredAuthority, connection -> {
            try {
                getLogger().trace("Running select query in repository: {}", connection.getRepository());

                TupleQuery q = connection.prepareTupleQuery(QueryLanguage.SPARQL, query);
                if (getLogger().isTraceEnabled())
                    getLogger().trace("Querying repository '{}'", connection.getRepository());
                try (TupleQueryResult result = q.evaluate()) {
                    Set<BindingSet> collect = result.stream().collect(Collectors.toSet());
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
    public Mono<Void> reset(Authentication authentication, RepositoryType repositoryType, GrantedAuthority requiredAuthority) {
        return this.consumeWithConnection(authentication, requiredAuthority, connection -> {
            try {
                if (!connection.isOpen() || connection.isActive()) return;

                if (getLogger().isTraceEnabled()) {
                    // RepositoryResult<Statement> statements = connection.getStatements(null, null, null);
                    getLogger().trace("Removing {} statements from repository '{}'", connection.size(), connection.getRepository());
                }

                connection.clear();

                if (!connection.isEmpty())
                    throw new RepositoryException("Repository not empty after clearing");

            } catch (Exception e) {
                getLogger().error("Failed to clear repository: {}", connection.getRepository());
                throw e;
            }
        });
    }


    @Override
    public Mono<Void> delete(Model model, Authentication authentication, GrantedAuthority requiredAuthority) {
        return this.consumeWithConnection(authentication, requiredAuthority, connection -> {
            try {
                Resource[] contexts = model.contexts().toArray(new Resource[0]);
                connection.add(model, contexts);
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        });
    }


    InputStream getInputStreamFromFluxDataBuffer(Publisher<DataBuffer> data) throws IOException {
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
    public Mono<Void> importStatements(Publisher<DataBuffer> bytesPublisher, String mimetype, Authentication authentication, GrantedAuthority requiredAuthority) {

        Optional<RDFParserFactory> parserFactory = RdfUtils.getParserFactory(MimeType.valueOf(mimetype));
        Assert.isTrue(parserFactory.isPresent(), "Unsupported mimetype for parsing the file.");

        RDFParser parser = parserFactory.orElseThrow().getParser();

        return this.consumeWithConnection(authentication, requiredAuthority, connection -> {
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
            } finally {
            }
        });

    }

    public Flux<IRI> types(Resource subj, Authentication authentication, GrantedAuthority requiredAuthority) {
        return this.applyManyWithConnection(authentication, requiredAuthority, connection ->
                connection.getStatements(subj, RDF.TYPE, null, false).stream()
                        .map(Statement::getObject)
                        .filter(Value::isIRI)
                        .map(value -> (IRI) value)
                        .collect(Collectors.toSet()));
    }


    @Override
    public Flux<RdfTransaction> commit(Collection<RdfTransaction> transactions, Authentication authentication, GrantedAuthority requiredAuthority) {
        return this.applyManyWithConnection(authentication, requiredAuthority, connection -> {
            connection.begin();
            Set<RdfTransaction> result = transactions.stream().peek(trx -> {
                synchronized (connection) {
                    getLogger().trace("Committing transaction '{}' to repository '{}'", trx.getIdentifier().getLocalName(), connection.getRepository().toString());
                    // FIXME: the approach based on the context works only as long as the statements in the graph are all within the global context only
                    // with this approach, we cannot insert a statement to a context (since it is already in GRAPH_CREATED), every st can only be in one context
                    ValueFactory vf = connection.getValueFactory();
                    Set<Statement> insertStatements = trx.getModel().filter(null, null, null, Transactions.GRAPH_CREATED).stream().map(s -> vf.createStatement(s.getSubject(), s.getPredicate(), s.getObject())).collect(Collectors.toSet());
                    Set<Statement> removeStatements = trx.getModel().filter(null, null, null, Transactions.GRAPH_DELETED).stream().map(s -> vf.createStatement(s.getSubject(), s.getPredicate(), s.getObject())).collect(Collectors.toSet());
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
    public Mono<Void> insert(Model model, Authentication authentication, GrantedAuthority requiredAuthority) {
        return this.consumeWithConnection(authentication, requiredAuthority, connection -> {
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
    public Mono<Set<Statement>> listStatements(Resource value, IRI predicate, Value object, Authentication authentication, GrantedAuthority requiredAuthority) {
        return this.applyWithConnection(authentication, requiredAuthority, connection -> {
            if (getLogger().isTraceEnabled()) {
                getLogger().trace("Listing all statements with pattern [{},{},{}] from repository '{}'", value, predicate, object, connection.getRepository().toString());
            }

            Set<Statement> statements = connection.getStatements(value, predicate, object).stream().collect(Collectors.toUnmodifiableSet());
            return statements;
        });

    }

    @Override
    public Mono<Boolean> hasStatement(Resource value, IRI predicate, Value object, Authentication authentication, GrantedAuthority requiredAuthority) {
        return this.applyWithConnection(authentication, requiredAuthority, connection -> connection.hasStatement(value, predicate, object, false));

    }




    @Override
    public Mono<Boolean> exists(Resource subj, Authentication authentication, GrantedAuthority requiredAuthority) {
        return this.applyWithConnection(authentication, requiredAuthority, connection -> connection.hasStatement(subj, RDF.TYPE, null, false));
    }

    @Override
    public Mono<RdfTransaction> insert(Model model, RdfTransaction transaction) {
        Assert.notNull(transaction, "Transaction cannot be null");
        if (getLogger().isTraceEnabled())
            getLogger().trace("Insert planned for {} statements in transaction '{}'.", model.size(), transaction.getIdentifier().getLocalName());


        transaction = transaction
                .insert(model, Activity.INSERTED)
                .affected(model);

        return transaction.asMono();
    }


    @Override
    public Mono<RdfTransaction> removeStatements(Collection<Statement> statements, RdfTransaction transaction) {
        Assert.notNull(transaction, "Transaction cannot be null");
        if (getLogger().isTraceEnabled())
            getLogger().trace("Removal planned for {} statements in transaction '{}'.", statements.size(), transaction.getIdentifier().getLocalName());

        return transaction
                .remove(statements, Activity.REMOVED)
                .asMono();
    }

    @Override
    public Mono<RdfTransaction> addStatement(Resource subject, IRI predicate, Value literal, RdfTransaction transaction) {
        return this.addStatement(subject, predicate, literal, null, transaction);
    }

    @Override
    public Mono<RdfTransaction> addStatement(Resource subject, IRI predicate, Value literal, @Nullable Resource context, RdfTransaction transaction) {
        if (getLogger().isTraceEnabled())
            getLogger().trace("Marking statement for insert in transaction {}: {} - {} - {}", transaction.getIdentifier().getLocalName(), subject, predicate, literal);


        return transaction
                .insert(subject, predicate, literal, context, Activity.UPDATED)
                .affected(subject, predicate, literal)
                .asMono();
    }


    protected <T> Mono<T> applyWithConnection(Authentication authentication, GrantedAuthority requiredAuthority, ThrowingFunction<RepositoryConnection, T> fun) {
        if (!Authorities.satisfies(requiredAuthority, authentication.getAuthorities())) {
            String msg = String.format("Required authority '%s' for repository '%s' not met in authentication with authorities '%s'", requiredAuthority.getAuthority(), repositoryType.name(), authentication.getAuthorities());
            return Mono.error(new InsufficientAuthenticationException(msg));
        }


        return transactionsMonoTimer.record(() ->
                this.getBuilder().buildRepository(this, authentication)
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


    protected Mono<Void> consumeWithConnection(Authentication authentication, GrantedAuthority requiredAuthority, ThrowingConsumer<RepositoryConnection> fun) {
        if (!Authorities.satisfies(requiredAuthority, authentication.getAuthorities())) {
            String msg = String.format("Required authority '%s' for repository '%s' not met in authentication with authorities '%s'", requiredAuthority.getAuthority(), repositoryType.name(), authentication.getAuthorities());
            return Mono.error(new InsufficientAuthenticationException(msg));
        }
        return transactionsMonoTimer.record(() ->
                this.getBuilder().buildRepository(this, authentication)
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

    protected <E, T extends Iterable<E>> Flux<E> applyManyWithConnection(Authentication authentication, GrantedAuthority requiredAuthority, ThrowingFunction<RepositoryConnection, T> fun) {
        if (!Authorities.satisfies(requiredAuthority, authentication.getAuthorities())) {
            String msg = String.format("Required authority '%s' for repository '%s' not met in authentication with authorities '%s'", requiredAuthority.getAuthority(), repositoryType.name(), authentication.getAuthorities());
            return Flux.error(new InsufficientAuthenticationException(msg));
        }

        return this.getBuilder().buildRepository(this, authentication)
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
                })
                .timeout(Duration.ofMillis(20000))
                .onErrorResume(throwable -> {
                    if (throwable instanceof TimeoutException te) {
                        getLogger().warn("Long-running operation on repository of type '{}' has been canceled.", repositoryType);
                        return Flux.error(new TimeoutException("Timeout while applying operation to repository:" + repositoryType.toString()));
                    } else {
                        return Flux.error(throwable);
                    }
                });

    }


}