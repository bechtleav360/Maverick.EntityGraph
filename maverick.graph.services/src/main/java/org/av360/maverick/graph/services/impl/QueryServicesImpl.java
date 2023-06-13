package org.av360.maverick.graph.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.services.QueryServices;
import org.av360.maverick.graph.store.behaviours.Searchable;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.query.parser.QueryParser;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.sparqlbuilder.core.query.ConstructQuery;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


@Service
@Slf4j(topic = "graph.srvc.query")
public class QueryServicesImpl implements QueryServices {

    private final Map<RepositoryType, Searchable> stores;



    private final QueryParser queryParser;

    public QueryServicesImpl(Set<Searchable> searchables) {
        this.stores = new HashMap<>();
        searchables.forEach(searchable -> stores.put(searchable.getRepositoryType(), searchable));

        queryParser = QueryParserUtil.createParser(QueryLanguage.SPARQL);
    }


    @Override
    public Flux<BindingSet> queryValues(String query, SessionContext ctx, RepositoryType repositoryType) {
        return Mono.just(query)
                .map(q -> queryParser.parseQuery(query, null))
                .filter(parsedQuery -> parsedQuery instanceof ParsedTupleQuery)
                .flatMapMany(parsedQuery -> this.stores.get(repositoryType).query(query, ctx));
    }

    @Override
    public Flux<BindingSet> queryValues(SelectQuery query, SessionContext ctx, RepositoryType repositoryType) {
        return this.stores.get(repositoryType).query(query.getQueryString(), ctx);
    }

    @Override
    public Flux<AnnotatedStatement> queryGraph(String queryStr, SessionContext ctx, RepositoryType repositoryType) {
        return Mono.just(queryStr)
                .map(q -> queryParser.parseQuery(queryStr, null))
                .flatMapMany(ptq -> this.stores.get(repositoryType).construct(queryStr, ctx))
                .doOnSubscribe(subscription -> {
                    if (log.isTraceEnabled())
                        log.trace("Running construct query in {}: {}", repositoryType, queryStr);
                });

    }

    public Flux<AnnotatedStatement> queryGraph(ConstructQuery query, SessionContext ctx, RepositoryType repositoryType) {
        return this.stores.get(repositoryType).construct(query.getQueryString(), ctx)
                .doOnSubscribe(subscription -> {
                    if (log.isTraceEnabled())
                        log.trace("Running construct query in {}: {}", repositoryType, query.getQueryString().replace('\n', ' ').trim());
                });
    }

}
