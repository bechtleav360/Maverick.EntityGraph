package io.av360.maverick.graph.store.rdf4j.repository.util;

import io.av360.maverick.graph.model.enums.Activity;
import io.av360.maverick.graph.model.rdf.NamespaceAwareStatement;
import io.av360.maverick.graph.model.vocabulary.Transactions;
import io.av360.maverick.graph.store.RepositoryBuilder;
import io.av360.maverick.graph.store.RepositoryType;
import io.av360.maverick.graph.store.behaviours.ModelUpdates;
import io.av360.maverick.graph.store.behaviours.RepositoryBehaviour;
import io.av360.maverick.graph.store.behaviours.Resettable;
import io.av360.maverick.graph.store.behaviours.Statements;
import io.av360.maverick.graph.store.rdf.helpers.RdfUtils;
import io.av360.maverick.graph.store.rdf.models.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.util.RDFInserter;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFParserFactory;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.web.client.HttpClientErrorException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j(topic = "graph.repo.base")
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


    public Flux<NamespaceAwareStatement> construct(String query, Authentication authentication, GrantedAuthority requiredAuthority) {
        return Flux.create(c -> {
            try (RepositoryConnection connection = getConnection(authentication, requiredAuthority)) {
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

    public Flux<BindingSet> query(String query, Authentication authentication, GrantedAuthority requiredAuthority) {
        return Flux.create(emitter -> {
            try (RepositoryConnection connection = this.getConnection(authentication, requiredAuthority)) {

                TupleQuery q = connection.prepareTupleQuery(QueryLanguage.SPARQL, query);
                if (log.isTraceEnabled())
                    log.trace("Querying repository '{}' with query: {}", connection.getRepository(), query.replace('\n', ' ').trim());
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
    public Mono<Void> reset(Authentication authentication, RepositoryType repositoryType, GrantedAuthority requiredAuthority) {
        try (RepositoryConnection connection = getConnection(authentication, repositoryType, requiredAuthority)) {
            if (log.isTraceEnabled())
                log.trace("Removing all statements from repository '{}'", connection.getRepository());
            RepositoryResult<Statement> statements = connection.getStatements(null, null, null);
            connection.remove(statements);
            return Mono.empty();
        } catch (Exception e) {
            return Mono.error(e);
        }
    }


    @Override
    public Mono<Void> delete(Model model, Authentication authentication, GrantedAuthority requiredAuthority) {
        return Mono.create(sink -> {
            try (RepositoryConnection connection = getConnection(authentication, requiredAuthority)) {
                try {
                    Resource[] contexts = model.contexts().toArray(new Resource[0]);
                    connection.add(model, contexts);
                    connection.commit();
                    sink.success();
                } catch (Exception e) {
                    connection.rollback();
                    sink.error(e);
                }
            } catch (RepositoryException | IOException e) {
                sink.error(e);
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
                        log.trace("Finished reading data buffers from publisher during import of data.");
                        osPipe.close();
                    } catch (IOException ignored) {
                    }
                })
                .doOnSubscribe(subscription -> log.trace("Starting reading data buffers from publisher during import of data."))
                .subscribe(DataBufferUtils.releaseConsumer());
        return isPipe;
    }

    @Override
    public Mono<Void> importStatements(Publisher<DataBuffer> bytesPublisher, String mimetype, Authentication authentication, GrantedAuthority requiredAuthority) {
        return Mono.create(snk -> {
            Optional<RDFParserFactory> parserFactory = RdfUtils.getParserFactory(MimeType.valueOf(mimetype));
            Assert.isTrue(parserFactory.isPresent(), "Unsupported mimetype for parsing the file.");

            RDFParser parser = parserFactory.orElseThrow().getParser();

            try (RepositoryConnection connection = getConnection(authentication, requiredAuthority)) {
                // example: https://www.baeldung.com/spring-reactive-read-flux-into-inputstream
                // solution: https://manhtai.github.io/posts/flux-databuffer-to-inputstream/
                log.trace("Starting to parse input stream with mimetype {}", mimetype);

                RDFInserter rdfInserter = new RDFInserter(connection);
                parser.setRDFHandler(rdfInserter);

                InputStream stream = getInputStreamFromFluxDataBuffer(bytesPublisher);
                parser.parse(stream);

                log.trace("Stored imported rdf into repository '{}'", connection.getRepository().toString());
                snk.success();

            } catch (Exception exception) {
                log.error("Failed to import statements with mimetype {} with reason: ", mimetype, exception);
                snk.error(exception);
            }
        });





                /*
        return DataBufferUtils.join(bytesPublisher)
                .flatMap(dataBuffer -> {
                    try (RepositoryConnection connection = getConnection(authentication, requiredAuthority)) {
                        RDFInserter rdfInserter = new RDFInserter(connection);
                        parser.setRDFHandler(rdfInserter);
                        try (InputStream bais = dataBuffer.asInputStream(true)) {
                            parser.parse(bais);
                        } catch (Exception e) {
                            return Mono.error(e);
                        }
                        return Mono.empty();
                    } catch (Exception e) {
                        return Mono.error(e);
                    }
                })
                .doOnError(throwable -> log.error("Error while importing statements: {}", throwable.getMessage()))
                .doOnSubscribe(subscription -> {
                    if (log.isTraceEnabled())
                        log.trace("Attempt to import statements into repository '{}'", this.getRepositoryType().name());
                })
                .then()
                ;
                */

    }

    public Flux<IRI> types(Resource subj, Authentication authentication, GrantedAuthority requiredAuthority) {
        try (RepositoryConnection connection = getConnection(authentication, requiredAuthority)) {

            Stream<Statement> stream = connection.getStatements(subj, RDF.TYPE, null, false).stream();
            return Flux.fromStream(stream)
                    .map(Statement::getObject)
                    .filter(Value::isIRI)
                    .map(value -> (IRI) value);
        } catch (IOException e) {
            return Flux.error(e);
        }
    }

    public Mono<Boolean> exists(Resource subj, Authentication authentication, GrantedAuthority requiredAuthority)  {
        try (RepositoryConnection connection = getConnection(authentication, requiredAuthority)) {
            return Mono.just(connection.hasStatement(subj, RDF.TYPE, null, false));
        } catch (IOException e) {
            return Mono.error(e);
        }
    }

    @Override
    public Flux<Transaction> commit(Collection<Transaction> transactions, Authentication authentication, GrantedAuthority requiredAuthority) {
        return Flux.create(c -> {
            try (RepositoryConnection connection = this.getConnection(authentication, requiredAuthority)) {


                log.trace("Committing transaction to repository '{}'", connection.getRepository().toString());

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

                        log.trace("Transaction completed with {} inserted statements and {} removed statements in repository '{}'.", insertModel.size(), removeModel.size(), connection.getRepository());
                        c.next(trx);
                    } catch (Exception e) {
                        log.error("Failed to complete transaction for repository '{}'.", connection.getRepository(), e);
                        log.trace("Insert Statements in this transaction: \n {}", insertModel);
                        log.trace("Remove Statements in this transaction: \n {}", removeModel);

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
    public Mono<Void> insert(Model model, Authentication authentication, GrantedAuthority requiredAuthority) {

        try (RepositoryConnection connection = this.getConnection(authentication, requiredAuthority)) {
            try {

                if (log.isTraceEnabled())
                    log.trace("Inserting model without transaction to repository '{}'", connection.getRepository().toString());

                Resource[] contexts = model.contexts().toArray(new Resource[0]);
                connection.add(model, contexts);
                connection.commit();
                return Mono.empty();
            } catch (Exception e) {
                connection.rollback();
                return Mono.error(e);
            }
        } catch (RepositoryException | IOException e) {
            return Mono.error(e);
        }

    }


    @Override
    public Mono<List<Statement>> listStatements(Resource value, IRI predicate, Value object, Authentication authentication, GrantedAuthority requiredAuthority) {
        try (RepositoryConnection connection = getConnection(authentication, requiredAuthority)) {
            if (log.isTraceEnabled())
                log.trace("Listing all statements with pattern [{},{},{}] from repository '{}'", value, predicate, object, connection.getRepository().toString());

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