package org.av360.maverick.graph.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.rdf.AnnotatedStatement;
import org.av360.maverick.graph.services.QueryServices;
import org.av360.maverick.graph.store.EntityStore;
import org.av360.maverick.graph.store.RepositoryType;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.parser.QueryParser;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.sparqlbuilder.core.query.ConstructQuery;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Service
@Slf4j(topic = "graph.srvc.query")
public class QueryServicesImpl implements QueryServices {

    private final EntityStore entityStore;


    private final QueryParser queryParser;

    public QueryServicesImpl(EntityStore graph) {
        this.entityStore = graph;

        queryParser = QueryParserUtil.createParser(QueryLanguage.SPARQL);
    }


    @Override
    public Flux<BindingSet> queryValues(String query, Authentication authentication, RepositoryType repositoryType) {
        return Mono.just(query)
                .map(q -> queryParser.parseQuery(query, null))
                .flatMapMany(ptq -> this.entityStore.query(query, authentication))
                .doOnSubscribe(subscription -> {
                    if (log.isTraceEnabled())
                        log.trace("Running select query in {}: {}", this.entityStore.getRepositoryType(), query.replace('\n', ' ').trim());
                });
    }

    @Override
    public Flux<BindingSet> queryValues(SelectQuery query, Authentication authentication, RepositoryType repositoryType) {
        return this.entityStore.query(query.getQueryString(), authentication)
                .doOnSubscribe(subscription -> {
                    if (log.isTraceEnabled())
                        log.trace("Running select query in {}:  {}", repositoryType, query.getQueryString().replace('\n', ' ').trim());
                });
    }

    @Override
    public Flux<AnnotatedStatement> queryGraph(String queryStr, Authentication authentication, RepositoryType repositoryType) {
        return Mono.just(queryStr)
                .map(q -> queryParser.parseQuery(queryStr, null))
                .flatMapMany(ptq -> this.entityStore.construct(queryStr, authentication))
                .doOnSubscribe(subscription -> {
                    if (log.isTraceEnabled())
                        log.trace("Running construct query in {}: {}", repositoryType, queryStr);
                });

    }

    public Flux<AnnotatedStatement> queryGraph(ConstructQuery query, Authentication authentication, RepositoryType repositoryType) {
        return this.entityStore.construct(query.getQueryString(), authentication)
                .doOnSubscribe(subscription -> {
                    if (log.isTraceEnabled())
                        log.trace("Running construct query in {}: {}", repositoryType, query.getQueryString().replace('\n', ' ').trim());
                });
    }

}
