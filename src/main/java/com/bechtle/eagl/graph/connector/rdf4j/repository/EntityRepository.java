package com.bechtle.eagl.graph.connector.rdf4j.repository;

import com.bechtle.eagl.graph.model.NamespaceAwareStatement;
import com.bechtle.eagl.graph.model.Transaction;
import com.bechtle.eagl.graph.repository.Graph;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashSet;
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
    public Flux<NamespaceAwareStatement> store(IRI subject, IRI predicate, Value literal) {
        Transaction transaction = new Transaction();
        return Flux.create(c -> {
            try(RepositoryConnection connection = repository.getConnection()) {
                try {
                    connection.begin();
                    connection.add(repository.getValueFactory().createStatement(subject, predicate, literal));
                    transaction.addModifiedResource(subject);
                    connection.commit();
                    transaction.stream().forEach(c::next);
                } catch (Exception e) {
                    log.warn("Error while loading statements, performing rollback.", e);
                    connection.rollback();
                    c.error(e);
                } finally {

                    c.complete();
                }
            }

        });

    }



    @Override
    public Flux<NamespaceAwareStatement> store(Model model) {
        /* create a new transaction node with some metadata, which is returned as object */

        Transaction transaction = new Transaction();

        return Flux.create(c -> {
            // Rio.write(triples.getStatements(), Rio.createWriter(RDFFormat.NQUADS, System.out));


            // TODO: perform validation via sha
            // https://rdf4j.org/javadoc/3.2.0/org/eclipse/rdf4j/sail/shacl/ShaclSail.html


            // get statements and load into repo
            try(RepositoryConnection connection = repository.getConnection()) {
                try {

                    connection.begin();
                    for(Resource obj : new ArrayList<>(model.subjects())) {
                        if(obj.isIRI()) {
                            connection.add(model.getStatements(obj, null, null));
                            transaction.addModifiedResource(obj);
                        }
                    }
                    connection.commit();
                    transaction.stream().forEach(c::next);

                } catch (Exception e) {
                    log.warn("Error while loading statements, performing rollback.", e);
                    connection.rollback();
                    c.error(e);
                } finally {

                    c.complete();
                }
            }

        });
    }

    @Override
    public Flux<NamespaceAwareStatement> get(IRI id) {
        return Flux.create(c -> {
            try(RepositoryConnection connection = repository.getConnection()) {
                try {
                    RepositoryResult<Statement> result = connection.getStatements(id, null, null);
                    // FIXME: predefine set of namespaces
                    Set<Namespace> namespaces = new HashSet<>();

                    result.forEach(statement -> c.next(NamespaceAwareStatement.wrap(statement, namespaces)));
                } catch (Exception e) {
                    log.warn("Error while loading statements, performing rollback.", e);
                    connection.rollback();
                }
            } catch (Exception e) {
                log.error("Unknown error while running query", e);
                c.error(e);
            } finally {
                c.complete();
            }
        });
    }

    @Override
    public Mono<TupleQueryResult> queryValues(String query) {
        return Mono.create(c -> {
            try(RepositoryConnection connection = repository.getConnection()) {
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
            try(RepositoryConnection connection = repository.getConnection()) {
                GraphQuery q = connection.prepareGraphQuery(QueryLanguage.SPARQL, query);
                try (GraphQueryResult result = q.evaluate()) {
                    Set<Namespace> namespaces = result.getNamespaces().entrySet().stream()
                            .map(entry -> new SimpleNamespace(entry.getKey(), entry.getValue()))
                            .collect(Collectors.toSet());

                    result.forEach(statement -> c.next(NamespaceAwareStatement.wrap(statement, namespaces)));
                }  catch (Exception e) {
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
            finally {
                c.complete();
            }
        });
    }

    @Override
    public ValueFactory getValueFactory() {
        return this.repository.getValueFactory();
    }



}
