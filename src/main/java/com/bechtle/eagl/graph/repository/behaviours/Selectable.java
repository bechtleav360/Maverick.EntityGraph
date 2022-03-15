package com.bechtle.eagl.graph.repository.behaviours;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import reactor.core.publisher.Mono;


public interface Selectable extends RepositoryBehaviour {


    default Mono<TupleQueryResult> select(String query) {
        return Mono.create(m -> {
            try (RepositoryConnection connection = getRepository().getConnection()) {
                TupleQuery q = connection.prepareTupleQuery(QueryLanguage.SPARQL, query);
                try (TupleQueryResult result = q.evaluate()) {
                    m.success(result);
                } catch (Exception e) {
                    m.error(e);
                }
            } catch (Exception e) {
                m.error(e);
            }
        });
    }

    default Mono<TupleQueryResult> select(SelectQuery all) {
        return this.select(all.getQueryString());
    }
}
