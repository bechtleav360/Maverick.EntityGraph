package com.bechtle.eagl.graph.repository.rdf4j.repository;

import com.bechtle.eagl.graph.domain.model.extensions.NamespaceAwareStatement;
import com.bechtle.eagl.graph.domain.model.wrapper.Entity;
import com.bechtle.eagl.graph.domain.model.wrapper.Transaction;
import com.bechtle.eagl.graph.repository.Graph;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class EntityRepository implements Graph {

    private final Repository repository;

    public EntityRepository(Repository repository) {
        this.repository = repository;
    }


    @Override
    public Mono<Transaction> store(Resource subject, IRI predicate, Value literal, @Nullable Transaction transaction) {
        if (transaction == null) transaction = new Transaction();

        Transaction finalTransaction = transaction;
        return Mono.create(c -> {
            try (RepositoryConnection connection = repository.getConnection()) {
                try {
                    connection.begin();
                    connection.add(repository.getValueFactory().createStatement(subject, predicate, literal));
                    finalTransaction.addModifiedResource(subject);
                    connection.commit();
                    c.success(finalTransaction);
                } catch (Exception e) {
                    log.warn("Error while loading statements, performing rollback.", e);
                    connection.rollback();
                    c.error(e);
                }
            }

        });

    }


    @Override
    public Mono<Transaction> store(Model model, Transaction transaction) {
        /* create a new transaction node with some metadata, which is returned as object */

        return Mono.create(c -> {
            // Rio.write(triples.getStatements(), Rio.createWriter(RDFFormat.NQUADS, System.out));

            // TODO: perform validation via sha
            // https://rdf4j.org/javadoc/3.2.0/org/eclipse/rdf4j/sail/shacl/ShaclSail.html


            // get statements and load into repo
            try (RepositoryConnection connection = repository.getConnection()) {
                try {


                    connection.begin();
                    for (Resource obj : new ArrayList<>(model.subjects())) {
                        if (obj.isIRI()) {
                            connection.add(model.getStatements(obj, null, null));
                            transaction.addModifiedResource(obj);
                        }
                    }
                    connection.commit();
                    c.success(transaction);

                } catch (Exception e) {
                    log.warn("Error while loading statements, performing rollback.", e);
                    connection.rollback();
                    c.error(e);
                }
            }

        });
    }


    @Override
    public Mono<Entity> get(IRI id) {
        return Mono.create(c -> {
            try (RepositoryConnection connection = repository.getConnection()) {
                Entity entity = new Entity()
                        .withResult(connection.getStatements(id, null, null));

                // embedded level 1
                entity.getModel().objects().stream()
                        .filter(Value::isIRI)
                        .map(value -> connection.getStatements((IRI) value, null, null))
                        .toList()
                        .forEach(entity::withResult);


                if (entity.getModel().size() > 0) c.success(entity);
                else c.success();

            } catch (Exception e) {
                log.error("Unknown error while running query", e);
                c.error(e);
            }
        });
    }

    @Override
    public Mono<TupleQueryResult> queryValues(String query) {
        return Mono.create(c -> {
            try (RepositoryConnection connection = repository.getConnection()) {
                TupleQuery q = connection.prepareTupleQuery(QueryLanguage.SPARQL, query);
                try (TupleQueryResult result = q.evaluate()) {
                    c.success(result);
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
            }
        });
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


}
