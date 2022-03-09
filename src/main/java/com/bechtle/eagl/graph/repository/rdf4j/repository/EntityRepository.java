package com.bechtle.eagl.graph.repository.rdf4j.repository;

import com.bechtle.eagl.graph.api.converter.RdfUtils;
import com.bechtle.eagl.graph.domain.model.enums.Activity;
import com.bechtle.eagl.graph.domain.model.extensions.NamespaceAwareStatement;
import com.bechtle.eagl.graph.domain.model.vocabulary.Transactions;
import com.bechtle.eagl.graph.domain.model.wrapper.Entity;
import com.bechtle.eagl.graph.domain.model.wrapper.Transaction;
import com.bechtle.eagl.graph.repository.EntityStore;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.util.RDFInserter;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFParserFactory;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.web.client.HttpClientErrorException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Component
public class EntityRepository implements EntityStore {

    private final Repository repository;

    public EntityRepository(@Qualifier("entities-storage") Repository repository) {
        this.repository = repository;
    }



    /*
    @Override
    public Mono<Transaction> store(Resource subject, IRI predicate, Value literal, @Nullable Transaction transaction) {
        if (transaction == null) transaction = new Transaction();

        Transaction finalTransaction = transaction;
        return Mono.create(c -> {
            try (RepositoryConnection connection = repository.getConnection()) {
                try {

                    Statement statement = repository.getValueFactory().createStatement(subject, predicate, literal);
                    connection.begin();
                    connection.add(statement);
                    connection.commit();

                    finalTransaction.withInsertedResource(subject);
                    finalTransaction.affected(NamespaceAwareStatement.wrap(statement, Collections.emptySet()));
                    if (log.isDebugEnabled()) log.debug("(Store) Transaction completed for storing one statement.");

                    c.success(finalTransaction);
                } catch (Exception e) {
                    log.warn("Error while storing statement, performing rollback.", e);
                    connection.rollback();
                    c.error(e);
                }
            }

        });

    }
     */


    /*
    @Override
    public Mono<Transaction> store(Model model, Transaction transaction) {

        return Mono.create(c -> {
            // Rio.write(triples.getStatements(), Rio.createWriter(RDFFormat.NQUADS, System.out));


            // each linked data fragment (sharing the same subject) is stored within the same transaction
            for (Resource obj : new ArrayList<>(model.subjects())) {
                transaction.withInsertedResource(obj);
            }


            // get statements and load into repo
            try (RepositoryConnection connection = repository.getConnection()) {
                try {

                    // each linked data fragment (sharing the same subject) is stored within the same transaction
                    for (Resource obj : new ArrayList<>(model.subjects())) {
                        connection.begin();
                        Iterable<Statement> statements = model.getStatements(obj, null, null);
                        connection.add(model.getStatements(obj, null, null));
                        connection.commit();

                        transaction.withInsertedResource(obj);
                        transaction.affected(statements); // we hold all statements in the transaction

                    }

                    if (log.isTraceEnabled())
                        log.trace("(Store) Transaction completed for storing a model with {} statements.", (long) model.size());
                    c.success(transaction);

                } catch (Exception e) {
                    log.warn("Error while loading statements, performing rollback.", e);
                    connection.rollback();
                    c.error(e);
                }
            }

        });
    }
    */


    @Override
    public Mono<Entity> get(IRI id) {
        try (RepositoryConnection connection = repository.getConnection()) {

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
    public Mono<TupleQueryResult> queryValues(String query) {
        return Mono.create(m -> {
            try (RepositoryConnection connection = repository.getConnection()) {
                TupleQuery q = connection.prepareTupleQuery(QueryLanguage.SPARQL, query);
                try (TupleQueryResult result = q.evaluate()) {
                    m.success(result);
                } catch (Exception e) {
                    log.warn("Error while running value query.", e);
                    m.error(e);
                }
            } catch (MalformedQueryException e) {
                log.warn("Error while parsing query, reason: {}", e.getMessage());
                m.error(e);
            } catch (Exception e) {
                log.error("Unknown error while running query", e);
                m.error(e);
            }
        });
    }



    @Override
    public Mono<TupleQueryResult> queryValues(SelectQuery all) {
        return this.queryValues(all.getQueryString());
    }

    @Override
    public Flux<NamespaceAwareStatement> queryStatements(String query) {
        return Flux.create(c -> {
            try (RepositoryConnection connection = repository.getConnection()) {
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
    public ValueFactory getValueFactory() {
        return this.repository.getValueFactory();
    }

    @Override
    public Mono<Boolean> exists(Resource subj) {
        return Mono.create(m -> {
            try (RepositoryConnection connection = repository.getConnection()) {
                boolean b = connection.hasStatement(subj, RDF.TYPE, null, false);
                m.success(b);
            } catch (Exception e) {
                m.error(e);
            }
        });
    }

    @Override
    public boolean existsSync(Resource subj) {
        try (RepositoryConnection connection = repository.getConnection()) {
            return connection.hasStatement(subj, RDF.TYPE, null, false);
        }
    }


    @Override
    public Mono<Value> type(Resource identifier) {
        return Mono.create(m -> {
            try (RepositoryConnection connection = repository.getConnection()) {
                RepositoryResult<Statement> statements = connection.getStatements(identifier, RDF.TYPE, null, false);

                Value result = null;
                for (Statement st : statements) {
                    // FIXME: not sure if this is a domain exception (which mean it should not be handled here)
                    if (result != null)
                        m.error(new IOException("Duplicate type definitions for resource with identifier " + identifier.stringValue()));
                    else result = st.getObject();
                }
                if (result == null) m.success();
                else m.success(result);


            } catch (Exception e) {
                m.error(e);
            }
        });
    }

    @Override
    public Mono<Void> reset() {
        return Mono.create(m -> {
            try (RepositoryConnection connection = repository.getConnection()) {
                RepositoryResult<Statement> statements = connection.getStatements(null, null, null);
                connection.remove(statements);
                m.success();
            } catch (Exception e) {
                m.error(e);
            }
        });
    }

    @Override
    public Mono<Void> importStatements(Publisher<DataBuffer> bytesPublisher, String mimetype) {
        Optional<RDFParserFactory> parserFactory = RdfUtils.getParserFactory(MimeType.valueOf(mimetype));
        Assert.isTrue(parserFactory.isPresent(), "Unsupported mimetype for parsing the file.");
        RDFParser parser = parserFactory.orElseThrow().getParser();


        Mono<DataBuffer> joined = DataBufferUtils.join(bytesPublisher);


        return joined.flatMap(bytes -> {
            try (RepositoryConnection connection = repository.getConnection()) {
                RDFInserter rdfInserter = new RDFInserter(connection);
                parser.setRDFHandler(rdfInserter);
                try (InputStream bais = bytes.asInputStream(true)) {
                    parser.parse(bais);
                } catch (Exception e) {
                    return Mono.error(e);
                }
                return Mono.empty();
            } catch (Exception e) {
                return Mono.error(e);
            }

        }).then();


    }

    @Override
    public Mono<List<Statement>> listStatements(Resource value, IRI predicate, Value object) {
        try (RepositoryConnection connection = repository.getConnection()) {
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
    public Mono<Transaction> insertModel(Model model, Transaction transaction) {
        Assert.notNull(transaction, "Transaction cannot be null");

        transaction = transaction
                .insert(model, Activity.INSERTED)
                .affected(model);


        return transaction.asMono();
    }


    @Override
    public Mono<Transaction> removeStatement(Resource subject, IRI predicate, Value value, Transaction transaction) {
        Assert.notNull(transaction, "Transaction cannot be null");

        return transaction
                .remove(subject, predicate, value, Activity.UPDATED)
                .affected(subject, predicate, value)
                .asMono();
    }


    @Override
    public Mono<Transaction> deleteModel(List<Statement> statements) {
        return this.deleteModel(statements, new Transaction());
    }


    @Override
    public Mono<Transaction> deleteModel(List<Statement> statements, Transaction transaction) {
        Assert.notNull(transaction, "Transaction cannot be null");

        // FIXME
        /*
        actor.core.Exceptions$ErrorCallbackNotImplemented: java.lang.NullPointerException: Cannot invoke "String.hashCode()" because the return value of "org.eclipse.rdf4j.model.base.AbstractIRI.stringValue()" is null
Caused by: java.lang.NullPointerException: Cannot invoke "String.hashCode()" because the return value of "org.eclipse.rdf4j.model.base.AbstractIRI.stringValue()" is null
        at org.eclipse.rdf4j.model.base.AbstractIRI.hashCode(AbstractIRI.java:39) ~[rdf4j-model-api-4.0.0-M2.jar:4.0.0-M2]
        at java.base/java.util.HashMap.hash(HashMap.java:338) ~[na:na]
        at java.base/java.util.HashMap.getNode(HashMap.java:568) ~[na:na]
        at java.base/java.util.HashMap.get(HashMap.java:556) ~[na:na]
        at org.eclipse.rdf4j.model.impl.LinkedHashModel.asNode(LinkedHashModel.java:554) ~[rdf4j-model-4.0.0-M2.jar:4.0.0-M2]
        at org.eclipse.rdf4j.model.impl.LinkedHashModel.add(LinkedHashModel.java:170) ~[rdf4j-model-4.0.0-M2.jar:4.0.0-M2]
        at com.bechtle.eagl.graph.domain.model.extensions.NamespacedModelBuilder.lambda$add$3(NamespacedModelBuilder.java:52) ~[classes/:na]
        at java.base/java.util.ArrayList.forEach(ArrayList.java:1511) ~[na:na]
        at com.bechtle.eagl.graph.domain.model.extensions.NamespacedModelBuilder.add(NamespacedModelBuilder.java:51) ~[classes/:na]
        at com.bechtle.eagl.graph.domain.model.wrapper.Transaction.remove(Transaction.java:54) ~[classes/:na]
        at com.bechtle.eagl.graph.repository.rdf4j.repository.EntityRepository.deleteModel(EntityRepository.java:
         */

        return transaction
                .remove(statements, Activity.REMOVED)
                .asMono();

        /*
        return Mono.create(c -> {
            try (RepositoryConnection connection = repository.getConnection()) {

                try {
                    connection.begin();
                    connection.remove(statements);
                    connection.commit();

                    if (log.isTraceEnabled())
                        log.trace("(Store) Transaction completed for removing {} statements.", (long) statements.size());
                    statements.stream().map(Statement::getSubject).forEach(transaction::withDeletedResource);

                    c.success(transaction);
                } catch (Exception e) {
                    log.trace("(Store) Failed to remove {} statements.", (long) statements.size(), e);
                    connection.rollback();
                    c.error(e);
                }
            }
        });
         */
    }

    @Override
    public Flux<Transaction> commit(Collection<Transaction> transactions) {
        return Flux.create(c -> {
            try (RepositoryConnection connection = repository.getConnection()) {
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
                        log.trace("(Store) Transaction completed with {} inserted statements and {} removed statements.", insertModel.size(), removeModel.size());
                        c.next(trx);
                    } catch (Exception e) {
                        log.error("(Store) Failed to complete transaction .", e);
                        log.trace("(Store) Insert Statements in this transaction: \n {}", insertModel);
                        log.trace("(Store) Remove Statements in this transaction: \n {}", removeModel);

                        connection.rollback();
                        trx.setFailed(e.getMessage());
                        c.next(trx);
                    }


                });

                c.complete();
            }
        });
    }


}
