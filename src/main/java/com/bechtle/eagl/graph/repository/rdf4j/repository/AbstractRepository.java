package com.bechtle.eagl.graph.repository.rdf4j.repository;

import com.bechtle.eagl.graph.domain.model.vocabulary.Transactions;
import com.bechtle.eagl.graph.domain.model.wrapper.Transaction;
import com.bechtle.eagl.graph.repository.behaviours.RepositoryBehaviour;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

@Slf4j
public class AbstractRepository implements AutoCloseable, RepositoryBehaviour {
    public final Repository repository;

    public AbstractRepository(Repository repository) {
        this.repository = repository;
    }


    public Mono<Void> store(Model model) {
        return Mono.create(c -> {
            try (RepositoryConnection connection = repository.getConnection()) {
                try {
                    Resource[] contexts = model.contexts().toArray(new Resource[model.contexts().size()]);
                    connection.add(model, contexts);
                    connection.commit();
                    c.success();
                } catch (Exception e) {
                    connection.rollback();
                    c.error(e);
                }
            }
        });
    }



    public Mono<TupleQueryResult> select(String query) {
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



    public Mono<TupleQueryResult> select(SelectQuery all) {
        return this.select(all.getQueryString());
    }

    @Override
    public void close() throws Exception {
        this.repository.shutDown();
    }

    @Override
    public Repository getRepository() {
        return this.repository;
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
