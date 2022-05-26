package com.bechtle.cougar.graph.repository.behaviours;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface Selectable extends RepositoryBehaviour {


    /*default Mono<TupleQueryResult> select(String query) {
        return getRepository().map(repository -> {
            try (RepositoryConnection connection = repository.getConnection()) {
                TupleQuery q = connection.prepareTupleQuery(QueryLanguage.SPARQL, query);
                try (TupleQueryResult result = q.evaluate()) {
                    return result;
                } catch (Exception e) {
                    throw e;
                }
            } catch (Exception e) {
                throw e;
            }
        });
    }*/

    default Flux<BindingSet> query(SelectQuery all) {
        return this.query(all.getQueryString());
    }

    Flux<BindingSet> query(String queryString);
}
