package com.bechtle.cougar.graph.repository.behaviours;

import com.bechtle.cougar.graph.domain.model.extensions.NamespaceAwareStatement;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Flux;


public interface Searchable extends RepositoryBehaviour {


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

    default Flux<BindingSet> query(SelectQuery all, Authentication authentication) {
        return this.query(all.getQueryString(), authentication);
    }

    Flux<BindingSet> query(String queryString, Authentication authentication);


    Flux<NamespaceAwareStatement> construct(String query, Authentication authentication);
}
